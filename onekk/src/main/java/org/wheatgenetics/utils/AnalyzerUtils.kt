package org.wheatgenetics.utils

//fun ByteBuffer.toByteArray(): ByteArray {
//    rewind()    // Rewind the buffer to zero
//    val data = ByteArray(remaining())
//    get(data)   // Copy the buffer into a byte array
//    return data // Return the byte array
//}

//fun ImageProxy.toBitmap(): Bitmap {
//
//    val buffer: ByteBuffer = this.planes[0].buffer
//    val bytes = ByteArray(buffer.remaining())
//
//    buffer.get(bytes)
//
//    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//}