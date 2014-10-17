/**
 * Copyright (C) 2014 Microsoft Corporation
 *
 * using https://github.com/Microsoft-CISL/shimoga
 * VectorCodec.java
 */

package edu.snu.bdcs.lbfgs;

import com.microsoft.wake.remote.Codec;

import javax.inject.Inject;
import java.io.*;

/**
 * Created by Jaehyun on 2014-10-17.
 */
public class LRArrayCodec implements Codec<LRArray> {
    @Inject
    public LRArrayCodec() {

    }

    @Override
    public LRArray decode(byte[] data) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        LRArray result;
        try (DataInputStream dais = new DataInputStream(bais)) {
            int size = dais.readInt();
            result = new LRArray(size);
            for (int i = 0; i < size; i++)
                result.set(i, dais.readDouble());
        } catch (IOException e) {
            throw new RuntimeException(e.getCause());
        }
        return result;
    }

    @Override
    public byte[] encode(LRArray arr) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(arr.size() * Double.SIZE);
        try (DataOutputStream daos = new DataOutputStream(baos)) {
            daos.writeInt(arr.size());
            for (int i = 0; i < arr.size(); i++) {
                daos.writeDouble(arr.get(i));
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getCause());
        }
        return baos.toByteArray();
    }
}
