package com.dodgingcat.app;

import java.util.concurrent.locks.LockSupport;

/** Absolute deadlines avoid drift and never enqueue a burst of stale render frames. */
public final class FramePacer {
    private final long period;
    private long deadline;
    public FramePacer(int framesPerSecond) {
        if(framesPerSecond<=0)throw new IllegalArgumentException("Positive frame rate required");
        period=Math.round(1_000_000_000.0/framesPerSecond);
    }
    public long awaitFrame() {
        long now=System.nanoTime();
        if(deadline==0||now-deadline>=period)deadline=now;
        while(now<deadline) {
            long remaining=deadline-now;
            if(remaining>500_000)LockSupport.parkNanos(remaining-250_000);
            else Thread.onSpinWait();
            now=System.nanoTime();
        }
        deadline+=period;
        return now;
    }
    public void reset(){deadline=0;}
}
