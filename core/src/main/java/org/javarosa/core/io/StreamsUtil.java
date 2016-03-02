package org.javarosa.core.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamsUtil {

    public static byte[] getStreamAsBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            StreamsUtil.writeFromInputToOutput(is, baos);
            return baos.toByteArray();
        } finally {
            baos.close();
        }
    }


    /**
     * Write everything from input stream to output stream, byte by byte then
     * close the streams
     */
    public static void writeFromInputToOutput(InputStream in, OutputStream out, long[] tally) throws InputIOException, OutputIOException {
        //TODO: God this is naive
        int val;
        try {
            val = in.read();
        } catch (IOException e) {
            throw new StreamsUtil().new InputIOException(e);
        }
        while (val != -1) {
            try {
                out.write(val);
            } catch (IOException e) {
                throw new StreamsUtil().new OutputIOException(e);
            }
            incr(tally);
            try {
                val = in.read();
            } catch (IOException e) {
                throw new StreamsUtil().new InputIOException(e);
            }
        }
    }

    public static void writeFromInputToOutputSpecific(InputStream in, OutputStream out) throws InputIOException, OutputIOException {
        writeFromInputToOutput(in, out, null);
    }

    public static void writeFromInputToOutput(InputStream in, OutputStream out) throws IOException {
        try {
            writeFromInputToOutput(in, out, null);
        } catch (InputIOException e) {
            throw e.internal;
        } catch (OutputIOException e) {
            throw e.internal;
        }
    }

    private static final int CHUNK_SIZE = 2048;

    /**
     * Write the byte array to the output stream
     */
    public static void writeToOutput(byte[] bytes, OutputStream out, long[] tally) throws IOException {
        int offset = 0;
        int remain = bytes.length;

        while (remain > 0) {
            int toRead = (remain < CHUNK_SIZE) ? remain : CHUNK_SIZE;
            out.write(bytes, offset, toRead);
            remain -= toRead;
            offset += toRead;
            if (tally != null) {
                tally[0] += toRead;
            }
        }
    }

    /**
     * Used by J2ME
     */
    public static void writeToOutput(byte[] bytes, OutputStream out) throws IOException {
        writeToOutput(bytes, out, null);
    }

    private static void incr(long[] tally) {
        if (tally != null) {
            tally[0]++;
        }
    }

    //Unify the functional aspects here

    private abstract class DirectionalIOException extends IOException {
        final IOException internal;

        public DirectionalIOException(IOException internal) {
            super(internal.getMessage());
            this.internal = internal;
        }

        public IOException getWrapped() {
            return internal;
        }

        public void printStackTrace() {
            internal.printStackTrace();
        }

        //TODO: Override all common methodss
    }

    public class InputIOException extends DirectionalIOException {
        public InputIOException(IOException internal) {
            super(internal);
        }
    }

    public class OutputIOException extends DirectionalIOException {
        public OutputIOException(IOException internal) {
            super(internal);
        }
    }
}
