package br.rnp.futebol.verona.exoplayerlegacy;

import android.os.Handler;
import android.os.SystemClock;

import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.SlidingPercentile;

/**
 * Imported from Google's Exoplayer on 26/04/17.
 */
public final class DefaultBandwidthMeter implements BandwidthMeter, TransferListener<Object> {

    /**
     * The default maximum weight for the sliding window.
     */
    public static final int DEFAULT_MAX_WEIGHT = 2000;

    private static final int ELAPSED_MILLIS_FOR_ESTIMATE = 2000;
    private static final int BYTES_TRANSFERRED_FOR_ESTIMATE = 512 * 1024;

    private final Handler eventHandler;
    private final EventListener eventListener;
    private final SlidingPercentile slidingPercentile;

    private int streamCount;
    private long sampleStartTimeMs;
    private long sampleBytesTransferred;

    private long totalElapsedTimeMs;
    private long totalBytesTransferred;
    private long bitrateEstimate;

    public DefaultBandwidthMeter() {
        this(null, null);
    }

    public DefaultBandwidthMeter(Handler eventHandler, EventListener eventListener) {
        this(eventHandler, eventListener, DEFAULT_MAX_WEIGHT);
    }

    public DefaultBandwidthMeter(Handler eventHandler, EventListener eventListener, int maxWeight) {
        this.eventHandler = eventHandler;
        this.eventListener = eventListener;
        this.slidingPercentile = new SlidingPercentile(maxWeight);
        bitrateEstimate = NO_ESTIMATE;
    }

    @Override
    public synchronized long getBitrateEstimate() {
        return bitrateEstimate;
    }

    public void setBitrateEstimate(long bitrateEstimate) {
        this.bitrateEstimate = bitrateEstimate;
    }

    @Override
    public synchronized void onTransferStart(Object source, DataSpec dataSpec) {
        if (streamCount == 0) {
            sampleStartTimeMs = SystemClock.elapsedRealtime();
        }
        streamCount++;
    }

    @Override
    public synchronized void onBytesTransferred(Object source, int bytes) {
        sampleBytesTransferred += bytes;
    }

    @Override
    public synchronized void onTransferEnd(Object source) {
        Assertions.checkState(streamCount > 0);
        long nowMs = SystemClock.elapsedRealtime();
        int sampleElapsedTimeMs = (int) (nowMs - sampleStartTimeMs);
        totalElapsedTimeMs += sampleElapsedTimeMs;
        totalBytesTransferred += sampleBytesTransferred;
        if (sampleElapsedTimeMs > 0) {
            float bitsPerSecond = (sampleBytesTransferred * 8000) / sampleElapsedTimeMs;
            slidingPercentile.addSample((int) Math.sqrt(sampleBytesTransferred), bitsPerSecond);
            if (totalElapsedTimeMs >= ELAPSED_MILLIS_FOR_ESTIMATE
                    || totalBytesTransferred >= BYTES_TRANSFERRED_FOR_ESTIMATE) {
                float bitrateEstimateFloat = slidingPercentile.getPercentile(0.5f);
                bitrateEstimate = Float.isNaN(bitrateEstimateFloat) ? NO_ESTIMATE
                        : (long) bitrateEstimateFloat;
            }
        }
        notifyBandwidthSample(sampleElapsedTimeMs, sampleBytesTransferred, bitrateEstimate);
        if (--streamCount > 0) {
            sampleStartTimeMs = nowMs;
        }
        sampleBytesTransferred = 0;
    }

    private void notifyBandwidthSample(final int elapsedMs, final long bytes, final long bitrate) {
        if (eventHandler != null && eventListener != null) {
            eventHandler.post(new Runnable()  {
                @Override
                public void run() {
                    eventListener.onBandwidthSample(elapsedMs, bytes, bitrate);
                }
            });
        }
    }

}