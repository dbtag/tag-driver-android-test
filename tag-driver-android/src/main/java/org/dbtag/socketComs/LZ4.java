package dbtag.socketComs;

class LZ4
  {
  static { System.loadLibrary("native"); }

  static native int compressLimitedOutput(byte[] srcArray, int srcOff, int inputSize, byte[] destArray, int destOff, int maxOutputSize);
  static native int decompressFast(byte[] srcArray, int srcOff, byte[] destArray, int destOff, int originalSize);
  static native int decompressSafe(byte[] srcArray, int srcOff, int compressedSize, byte[] destArray, int destOff, int maxDecompressedSize);
  static native int compressBound(int len);
  }
