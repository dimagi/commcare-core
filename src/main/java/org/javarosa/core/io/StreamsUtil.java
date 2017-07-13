package org.javarosa.core.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamsUtil {
    /**
     * Write everything from input stream to output stream, byte by byte then
     * close the streams
     */
    private static void writeFromInputToOutputInner(InputStream in, OutputStream out) throws InputIOException, OutputIOException {
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
            try {
                val = in.read();
            } catch (IOException e) {
                throw new StreamsUtil().new InputIOException(e);
            }
        }
    }

    public static void writeFromInputToOutputSpecific(InputStream in, OutputStream out) throws InputIOException, OutputIOException {
        writeFromInputToOutputInner(in, out);
    }

    public static void writeFromInputToOutput(InputStream in, OutputStream out) throws IOException {
        try {
            writeFromInputToOutputInner(in, out);
        } catch (InputIOException e) {
            throw e.internal;
        } catch (OutputIOException e) {
            throw e.internal;
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

        @Override
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

    public static byte[] inputStreamToByteArray(InputStream input) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        return output.toByteArray();
    }

    /**
     * Writes input stream to output stream in a buffered fashion, but doesn't
     * close either stream.
     */
    public static void writeFromInputToOutputUnmanaged(InputStream is, OutputStream os)
            throws InputIOException, OutputIOException {
        int count;
        byte[] buffer = new byte[8192];
        try {
            count = is.read(buffer);
        } catch (IOException e) {
            throw new StreamsUtil().new InputIOException(e);
        }
        while (count != -1) {
            try {
                os.write(buffer, 0, count);
            } catch (IOException e) {
                throw new StreamsUtil().new OutputIOException(e);
            }
            try {
                count = is.read(buffer);
            } catch (IOException e) {
                throw new StreamsUtil().new InputIOException(e);
            }
        }
    }

    /**
     * Write is to os and close both
     */
    public static void writeFromInputToOutputNew(InputStream is, OutputStream os) throws IOException {
        writeFromInputToOutputNew(is, os, null);
    }

    /**
     * Write is to os and close both
     */
    public static void writeFromInputToOutputNew(InputStream is, OutputStream os, StreamReadObserver observer) throws IOException {
        byte[] buffer = new byte[8192];
        long counter = 0;

        try {
            int count = is.read(buffer);
            while (count != -1) {
                counter += count;
                if (observer != null) {
                    observer.notifyCurrentCount(counter);
                }
                os.write(buffer, 0, count);
                count = is.read(buffer);
            }
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public interface StreamReadObserver {
        void notifyCurrentCount(long bytesRead);
    }

}
