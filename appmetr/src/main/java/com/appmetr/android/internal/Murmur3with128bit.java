package com.appmetr.android.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * Copyright (c) 2019 AppMetr.
 * All rights reserved.
 *
 * Murmur3with128bit was written by Austin Appleby, and is placed in the public
 * domain. The author hereby disclaims copyright to this source code.
 *
 * Source:
 * https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp
 * (This is minified version of 128bit implementation)
 */

public final class Murmur3with128bit {
    private static final int CHUNK_SIZE = 16;
    private static final long C1 = 0x87c37b91114253d5L;
    private static final long C2 = 0x4cf5ad432745937fL;
    private static final char[] hexDigits = "0123456789abcdef".toCharArray();
    private final ByteBuffer buffer;
    private long h1;
    private long h2;
    private int length;

    public Murmur3with128bit() {
        this(0);
    }

    public Murmur3with128bit(int seed) {
        this.buffer = ByteBuffer.allocate(CHUNK_SIZE + 7).order(ByteOrder.LITTLE_ENDIAN);
        this.h1 = seed;
        this.h2 = seed;
        this.length = 0;
    }

    private static long fmix64(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }

    private static long mixK1(long k1) {
        k1 *= C1;
        k1 = Long.rotateLeft(k1, 31);
        k1 *= C2;
        return k1;
    }

    private static long mixK2(long k2) {
        k2 *= C2;
        k2 = Long.rotateLeft(k2, 33);
        k2 *= C1;
        return k2;
    }

    public Murmur3with128bit putString(CharSequence charSequence) {
        return putString(charSequence, Charset.forName("UTF-8"));
    }

    public Murmur3with128bit putString(CharSequence charSequence, Charset charset) {
        return putBytes(charSequence.toString().getBytes(charset));
    }

    public Murmur3with128bit putBytes(byte[] bytes) {
        for (byte aByte : bytes) {
            putByte(aByte);
        }
        return this;
    }

    public Murmur3with128bit putByte(byte b) {
        buffer.put(b);
        if (buffer.remaining() < 8) {
            munch();
        }
        return this;
    }

    public byte[] hash() {
        munch();
        buffer.flip();
        if (buffer.remaining() > 0) {
            processRemaining(buffer);
            buffer.position(buffer.limit());
        }

        h1 ^= length;
        h2 ^= length;

        h1 += h2;
        h2 += h1;

        h1 = fmix64(h1);
        h2 = fmix64(h2);

        h1 += h2;
        h2 += h1;

        return ByteBuffer.wrap(new byte[CHUNK_SIZE])
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(h1)
                .putLong(h2)
                .array();
    }

    public String hashString() {
        byte[] hash = hash();
        StringBuilder sb = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            sb.append(hexDigits[(b >> 4) & 0xf]).append(hexDigits[b & 0xf]);
        }
        return sb.toString();
    }

    private void munch() {
        buffer.flip();
        while (buffer.remaining() >= CHUNK_SIZE) {
            process(buffer);
        }
        buffer.compact();
    }

    private void process(ByteBuffer bb) {
        long k1 = bb.getLong();
        long k2 = bb.getLong();
        bmix64(k1, k2);
        length += CHUNK_SIZE;
    }

    private void bmix64(long k1, long k2) {
        h1 ^= mixK1(k1);

        h1 = Long.rotateLeft(h1, 27);
        h1 += h2;
        h1 = h1 * 5 + 0x52dce729;

        h2 ^= mixK2(k2);

        h2 = Long.rotateLeft(h2, 31);
        h2 += h1;
        h2 = h2 * 5 + 0x38495ab5;
    }

    private void processRemaining(ByteBuffer bb) {
        long k1 = 0;
        long k2 = 0;
        length += bb.remaining();
        switch (bb.remaining()) {
            case 15:
                k2 ^= (long) (bb.get(14) & 0xFF) << 48;
            case 14:
                k2 ^= (long) (bb.get(13) & 0xFF) << 40;
            case 13:
                k2 ^= (long) (bb.get(12) & 0xFF) << 32;
            case 12:
                k2 ^= (long) (bb.get(11) & 0xFF) << 24;
            case 11:
                k2 ^= (long) (bb.get(10) & 0xFF) << 16;
            case 10:
                k2 ^= (long) (bb.get(9) & 0xFF) << 8;
            case 9:
                k2 ^= (long) (bb.get(8) & 0xFF);
            case 8:
                k1 ^= bb.getLong();
                break;
            case 7:
                k1 ^= (long) (bb.get(6) & 0xFF) << 48;
            case 6:
                k1 ^= (long) (bb.get(5) & 0xFF) << 40;
            case 5:
                k1 ^= (long) (bb.get(4) & 0xFF) << 32;
            case 4:
                k1 ^= (long) (bb.get(3) & 0xFF) << 24;
            case 3:
                k1 ^= (long) (bb.get(2) & 0xFF) << 16;
            case 2:
                k1 ^= (long) (bb.get(1) & 0xFF) << 8;
            case 1:
                k1 ^= (long) (bb.get(0) & 0xFF);
                break;
            default:
                throw new AssertionError("Something wrong");
        }
        h1 ^= mixK1(k1);
        h2 ^= mixK2(k2);
    }
}

