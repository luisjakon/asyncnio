package async.nio.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class Nio {

    public static class ByteBuffers {

        // PUT Methods
        public static ByteBuffer put(ByteBuffer buf, byte[] bytes) {
            buf.put(bytes);
            return buf;
        }

        public static ByteBuffer put(ByteBuffer buf, int[] ints) {
            for (int i : ints) {
                buf.putInt(i);
            }
            return buf;
        }

        public static ByteBuffer putOne(ByteBuffer buf, int value) {
            buf.put((byte) value);
            return buf;
        }

        public static ByteBuffer putTwo(ByteBuffer buf, int value) {
            buf.put((byte) (value >> 8));
            buf.put((byte) value);
            return buf;
        }

        public static ByteBuffer putFour(ByteBuffer buf, int value) {
            buf.putInt(value);
            return buf;
        }

        public static ByteBuffer put(ByteBuffer buf, int offset, byte[] bytes) {
            buf.put(bytes, offset, bytes.length);
            return buf;
        }

        public static ByteBuffer put(ByteBuffer buf, int offset, int[] ints) {
            int pos = buf.position();
            buf.position(buf.position() + offset);
            for (int i : ints) {
                buf.putInt(i);
            }
            buf.position(pos);
            return buf;
        }

        public static ByteBuffer putOne(ByteBuffer buf, int offset, int value) {
            buf.put(offset, (byte) value);
            return buf;
        }

        public static ByteBuffer putTwo(ByteBuffer buf, int offset, int value) {
            buf.put(offset, (byte) (value >> 8));
            buf.put(offset, (byte) value);
            return buf;
        }

        public static ByteBuffer putFour(ByteBuffer buf, int offset, int value) {
            buf.putInt(offset, value);
            return buf;
        }

        // GET Methods
        public static int get(ByteBuffer buf) {
            return buf.getInt();
        }

        public static int getOne(ByteBuffer buf) {
            return (int) buf.get();
        }

        public static int getTwo(ByteBuffer buf) {
            return (int) buf.getChar();
        }

        public static int getFour(ByteBuffer buf) {
            return buf.getInt();
        }

        public static int get(ByteBuffer buf, int offset) {
            return buf.getInt(offset);
        }

        public static int getOne(ByteBuffer buf, int offset) {
            return (int) buf.get(offset);
        }

        public static int getTwo(ByteBuffer buf, int offset) {
            return (int) buf.getChar(offset);
        }

        public static int getFour(ByteBuffer buf, int offset) {
            return buf.getInt(offset);
        }

        // ByteBuffer BitString Methods
        public static String toBitString(byte[] buf) {
            return toBitString(ByteBuffer.wrap(buf), 4);
        }

        public static String toBitString(ByteBuffer buf) {
            return toBitString(buf, 4);
        }

        public static String toBitString(byte[] buf, int bytesPerLine) {
            return toBitString(ByteBuffer.wrap(buf), bytesPerLine);
        }

        public static String toBitString(ByteBuffer buf, int bytesPerLine) {
            StringBuilder binary = new StringBuilder();
            int count = 0;
            while (buf.hasRemaining()) {
                byte b = buf.get();
                for (int i = 0; i < 8; i++) {
                    binary.append((b & 128) == 0 ? 0 : 1);
                    b <<= 1;
                }
                binary.append(" ");
                if (++count % bytesPerLine == 0) {
                    binary.append("\n");
                }
            }
            return binary.toString();
        }

        public static String toBitString(String file, int bytesPerLine) throws IOException {
            FileChannel ch = new RandomAccessFile(file, "r").getChannel();
            return toBitString(ch, bytesPerLine);

        }

        public static String toBitString(FileChannel file, int bytesPerLine) throws IOException {
            ByteBuffer b = ByteBuffer.allocate((int) file.size());
            file.read(b);
            b.rewind();
            return toBitString(b, bytesPerLine);

        }

        // ByteBuffer to ByteBuffer transfer.
        public static int transfer(ByteBuffer src, ByteBuffer dst) {
            int tot = Math.min(src.remaining(), dst.remaining());
            for (int i = 0; i < tot; i++) {
                dst.put(src.get());
            }
            return tot;
        }

        // ByteBuffer to ByteBuffer limited transfer
        public static int transfer(int amount, ByteBuffer src, int src_offset, ByteBuffer dst) {
            int pos = src.position();
            src.position(pos + src_offset);
            int tot = Math.min(src.remaining(), amount);
            tot = Math.min(dst.remaining(), amount);
            for (int i = 0; i < tot; i++) {
                dst.put(src.get());
            }
            src.position(pos);
            return tot;
        }

        // ByteBuffer to ByteBuffer[] transfer.
        public static int transfer(int amount, ByteBuffer src, ByteBuffer[] dst, int offset, int length) {
            // copy amount
            int rem = amount;
            for (; offset < length; offset++) {
                rem -= transfer(src, dst[offset]);
                if (rem <= 0) {
                    break;
                }
            }
            return rem;
        }

        // ByteBuffer[] to ByteBuffer transfer.
        public static int transfer(int amount, ByteBuffer[] src, int offset, int length, ByteBuffer dst) {
            // copy amount
            int rem = amount;
            for (; offset < length; offset++) {
                rem -= transfer(src[offset], dst);
                if (rem <= 0) {
                    break;
                }
            }
            return rem;
        }

        // ByteBuffer to ByteBuffer[] transfer.
        public static int transfer(ByteBuffer src, ByteBuffer[] dst, int offset, int length) {
            // copy as much as possible
            int tot = 0;
            for (; offset < length; offset++) {
                tot += transfer(src, dst[offset]);
                if (!src.hasRemaining()) {
                    break;
                }
            }
            return tot;
        }

        // ByteBuffer[] to ByteBuffer transfer.
        public static int transfer(ByteBuffer[] src, int offset, int length, ByteBuffer dst) {
            // copy as much as possible
            int tot = 0;
            for (; offset < length; offset++) {
                tot += transfer(src[offset], dst);
                if (!dst.hasRemaining()) {
                    break;
                }
            }
            return tot;
        }

        // ByteBuffer[] to ByteBuffer transfer.
        public static int transfer(byte[] src, ByteBuffer dst) {
            return transfer(ByteBuffer.wrap(src), dst);
        }

        public static boolean hasRemaining(final ByteBuffer[] list) {
            for (ByteBuffer b : list) {
                if (b.remaining() > 0) {
                    return true;
                }
            }
            return false;
        }

        public static int remaining(final ByteBuffer[] list) {
            return remaining(list, 0, list.length);
        }

        public static int remaining(final ByteBuffer[] list, int offset, int length) {
            int i = 0;
            for (int b = offset; b < length; b++) {
                i += list[b].remaining();
            }
            return i;
        }
    }

    private Nio() {
    } // Singleton
}
