package org.javarosa.core.io;

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
