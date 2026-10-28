package com.dodgingcat.app;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Offline packaging utility. The game still reads its ordinary PNG resources.
 * First launch restores losslessly packed images and verifies every ARGB pixel. */
public final class PortableAssets {
    private PortableAssets() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 3 && args[0].equals("--manifest")) {
            manifest(Path.of(args[1]), Path.of(args[2]));
        } else if(args.length==3&&args[0].equals("--audio-manifest")) {
            audioManifest(Path.of(args[1]),Path.of(args[2]));
        } else if (args.length == 1) {
            Path root=Path.of(args[0]).toAbsolutePath().normalize();
            restoreAudio(root);restore(root);
        } else throw new IllegalArgumentException("Usage: PortableAssets <portable-root> | --manifest <images> <packed>");
    }

    private static String fileHash(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }

    private static void audioManifest(Path audio,Path packed) throws Exception {
        Files.createDirectories(packed);var records=new ArrayList<String>();
        try(var paths=Files.walk(audio)) {
            for(Path file:paths.filter(p->p.toString().endsWith(".wav")).sorted().toList())
                records.add(audio.relativize(file).toString().replace('\\','/')+"\t"+fileHash(file));
        }
        Files.write(packed.resolve("audio.tsv"),records,StandardCharsets.UTF_8);
        System.out.println("Recorded "+records.size()+" original WAV file hashes.");
    }

    private static void restoreAudio(Path root) throws Exception {
        Path packed=root.resolve("packed-audio"),manifest=packed.resolve("audio.tsv"),marker=root.resolve(".audio-ready");
        Path audio=root.resolve("source/src/main/resources/assets/audio");
        var records=Files.readAllLines(manifest,StandardCharsets.UTF_8);String version=fileHash(manifest);
        if(Files.exists(marker)&&Files.readString(marker).equals(version)) {
            boolean complete=true;
            for(String record:records)if(!Files.isRegularFile(inside(audio,record.split("\t")[0]))) { complete=false;break; }
            if(complete)return;
        }
        Files.deleteIfExists(marker);int done=0;
        for(String record:records) {
            String[] item=record.split("\t");if(item.length!=2)throw new IOException("Invalid audio manifest");
            Path target=inside(audio,item[0]);Files.createDirectories(target.getParent());
            if(!Files.isRegularFile(target)||!fileHash(target).equals(item[1])) {
                Path temp=target.resolveSibling(target.getFileName()+".preparing.wav");
                var process=new ProcessBuilder(root.resolve("tools/wvunpack.exe").toString(),"-q","-y",
                        inside(packed,item[0]+".wv").toString(),"-o",temp.toString())
                        .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(root.resolve("prepare-assets.log").toFile())).start();
                if(process.waitFor()!=0||!Files.exists(temp)||!fileHash(temp).equals(item[1]))
                    throw new IOException("Audio integrity mismatch: "+item[0]);
                Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.printf("Preparing original audio: %d/%d%n",++done,records.size());
        }
        Files.writeString(marker,version,StandardCharsets.US_ASCII);
    }

    private static String pixels(Path file) throws Exception {
        var image=ImageIO.read(file.toFile());
        if(image==null)throw new IOException("Cannot decode "+file);
        var digest=MessageDigest.getInstance("SHA-256");
        digest.update(ByteBuffer.allocate(8).putInt(image.getWidth()).putInt(image.getHeight()).array());
        int[] row=new int[image.getWidth()];
        var bytes=ByteBuffer.allocate(row.length*4);
        for(int y=0;y<image.getHeight();y++) {
            image.getRGB(0,y,row.length,1,row,0,row.length);bytes.clear();
            for(int pixel:row)bytes.putInt(pixel);
            digest.update(bytes.array());
        }
        image.flush();return HexFormat.of().formatHex(digest.digest());
    }

    private static void manifest(Path images,Path packed) throws Exception {
        Files.createDirectories(packed);
        var records=new ArrayList<String>();
        try(var paths=Files.walk(images)) {
            for(Path file:paths.filter(p->p.toString().endsWith(".png")).sorted().toList()) {
                String name=images.relativize(file).toString().replace('\\','/');
                records.add(name+"\t"+pixels(file));
                Path header=inside(packed,name+".headers");Files.createDirectories(header.getParent());
                try(var in=new DataInputStream(Files.newInputStream(file));var out=Files.newOutputStream(header)) {
                    in.readNBytes(8);
                    while(in.available()>0) {
                        int length=in.readInt();byte[] type=in.readNBytes(4),data=in.readNBytes(length),crc=in.readNBytes(4);
                        String kind=new String(type,StandardCharsets.US_ASCII);
                        // Preserve display-affecting PNG metadata across the temporary codec.
                        if(Set.of("sRGB","gAMA","cHRM","iCCP","pHYs","cICP").contains(kind)) {
                            out.write(ByteBuffer.allocate(4).putInt(length).array());out.write(type);out.write(data);out.write(crc);
                        }
                    }
                }
            }
        }
        Files.write(packed.resolve("images.tsv"),records,StandardCharsets.UTF_8);
        System.out.println("Recorded "+records.size()+" exact pixel hashes and original display metadata.");
    }

    private static void restore(Path root) throws Exception {
        Path packed=root.resolve("packed-images"),manifest=packed.resolve("images.tsv");
        Path images=root.resolve("source/src/main/resources/assets/images"),marker=root.resolve(".images-ready");
        String version=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(manifest)));
        var records=Files.readAllLines(manifest,StandardCharsets.UTF_8);
        boolean ready=Files.exists(marker)&&Files.readString(marker).equals(version);
        if(ready) {
            for(String record:records) if(!Files.isRegularFile(inside(images,record.split("\t")[0]))) { ready=false;break; }
            if(ready)return;
        }
        Files.deleteIfExists(marker);
        Path decoder=root.resolve("tools/dwebp.exe"),log=root.resolve("prepare-assets.log");
        if(!Files.isRegularFile(decoder))throw new IOException("Missing bundled image decoder");
        int done=0;
        for(String record:records) {
            String[] item=record.split("\t");if(item.length!=2)throw new IOException("Invalid image manifest");
            Path target=inside(images,item[0]);Files.createDirectories(target.getParent());
            // An interrupted preparation can resume without overwriting already verified images.
            if(!matches(target,item[1])) {
                Path temp=target.resolveSibling(target.getFileName()+".preparing");
                Path original=inside(packed,item[0]+".original.png");
                if(Files.isRegularFile(original)) {
                    Files.copy(original,temp,StandardCopyOption.REPLACE_EXISTING);
                } else {
                var process=new ProcessBuilder(decoder.toString(),"-quiet",inside(packed,item[0]+".webp").toString(),"-o",temp.toString())
                        .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile())).start();
                if(process.waitFor()!=0)throw new IOException("Image unpack failed: "+item[0]+"; see "+log);
                byte[] png=Files.readAllBytes(temp),headers=Files.readAllBytes(inside(packed,item[0]+".headers"));
                if(png.length<33)throw new IOException("Invalid unpacked PNG: "+item[0]);
                try(var output=Files.newOutputStream(temp)) { output.write(png,0,33);output.write(headers);output.write(png,33,png.length-33); }
                }
                if(!pixels(temp).equals(item[1]))throw new IOException("Pixel integrity mismatch: "+item[0]);
                Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.printf("Preparing game images: %d/%d%n",++done,records.size());
        }
        Files.writeString(marker,version,StandardCharsets.US_ASCII);
    }

    private static boolean matches(Path image,String expected) {
        if(!Files.isRegularFile(image))return false;
        try { return pixels(image).equals(expected); } catch(Exception unreadable) { return false; }
    }

    private static Path inside(Path root,String name) throws IOException {
        Path base=root.toAbsolutePath().normalize(),file=base.resolve(name).normalize();
        if(!file.startsWith(base))throw new IOException("Invalid relative asset path");
        return file;
    }
}
