package mock.answers.readers;

import io.DataChunk;
import io.DataChunkInputStream;
import mock.answers.ReturnTypeAnswer;
import org.json.simple.JSONObject;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author Derrick Lockwood
 * @created 6/11/18.
 */
public abstract class ByteReader implements ReturnTypeAnswer {

    protected int chunkSize;
    private DataChunkInputStream inputStream;
    private Object object;
    private DataChunk dataChunk;

    public ByteReader(InputStream inputStream, int chunkSize) {
        if (inputStream != null) {
            this.inputStream = new DataChunkInputStream(inputStream);
        }
        this.chunkSize = chunkSize;
    }

    public static ByteReader createDefault() {
        return new DefaultByteReader();
    }

    private static ByteReader createRange(long start, long finish) {
        return new RangeByteReader(start, finish);
    }

    private static ByteReader createChunk(int chunkSize) {
        return new ChunkByteReader(chunkSize);
    }

    /**
     * Parses the type of the constraint and gets the type of byte reader that supplies the type.
     * <p>
     * If the constraint is null or the type is null the default byte reader is returned.
     * <p>
     * If the method's return type is void then a null empty answer is returned such that the
     * method's call reads in nothing.
     *
     * @param type
     * @param constraint
     * @return
     */
    public static ByteReader getByType(String type, JSONObject constraint) {
        if (type == null || constraint == null) {
            return createDefault();
        }
        switch (type) {
            case "range":
                return createRange((long) constraint.get("from"), (long) constraint.get("to"));
            case "chunk":
                return createChunk((int) constraint.get("size"));
            case "default":
                return createDefault();
            default:
                return createDefault();
        }
    }

    public void setInputStream(InputStream inputStream) throws IOException {
        object = null;
        if (this.inputStream != null)
            this.inputStream.close();
        this.inputStream = new DataChunkInputStream(inputStream);
    }

    @Override
    public Object createObject(Class<?> returnType, boolean forceReload) {
        if (returnType == null || inputStream == null) {
            return null;
        }
        if (!forceReload && dataChunk == null && object != null) {
            return object;
        }
        try {
            DataInput dataInput = this.inputStream;
            if (chunkSize >= 0 && dataChunk == null) {
                dataChunk = this.inputStream.readDataChunk(chunkSize);
            }
            if (dataChunk != null) {
                dataInput = dataChunk;
            }
            if (returnType.isAssignableFrom(byte.class)) {
                object = dataInput.readByte();
            } else if (returnType.isAssignableFrom(char.class)) {
                object = dataInput.readChar();
            } else if (returnType.isAssignableFrom(boolean.class)) {
                object = dataInput.readBoolean();
            } else if (returnType.isAssignableFrom(short.class)) {
                object = dataInput.readShort();
            } else if (returnType.isAssignableFrom(int.class)) {
                object = dataInput.readInt();
            } else if (returnType.isAssignableFrom(long.class)) {
                object = dataInput.readLong();
            } else if (returnType.isAssignableFrom(float.class)) {
                object = dataInput.readFloat();
            } else if (returnType.isAssignableFrom(double.class)) {
                object = dataInput.readDouble();
            } else if (returnType.isAssignableFrom(Void.class)) {
                object = null;
            } else {
                object = readNonPrimitiveClass(returnType, dataInput);
            }
        } catch (IOException e) {
            handleReadException(e);
            if (dataChunk != null) {
                dataChunk = null;
            }
        }
        if (dataChunk != null && dataChunk.isDoneReading()) {
            dataChunk = null;
        }
        object = postProcessing(returnType, object);
        return object;
    }

    @Override
    public String toString() {
        return Objects.toString(object);
    }

    abstract void handleReadException(IOException e);

    abstract Object readNonPrimitiveClass(Class<?> returnType, DataInput dataInput) throws IOException;

    abstract Object postProcessing(Class<?> returnType, Object object);

}