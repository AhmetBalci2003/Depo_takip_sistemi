package com.example.depo_takip_sistemi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.annotation.RequiresApi
import java.io.FileOutputStream
import java.io.IOException

//bu class pitmap tipindeki qr kodu pdf e cevirip yazdırma ekranına atar context
// ve bir bitmap ile printbitmap metodunu çağırın
class QR_print {
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun printBitmap(context: Context, bitmap: Bitmap) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onStart() {
                super.onStart()
            }
            override fun onWrite(
                pages: Array<out PageRange>?,
                printOutput: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                try {
                    printOutput?.let {
                        pdfDocument.writeTo(FileOutputStream(it.fileDescriptor))
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } ?: callback?.onWriteFailed("Error: PrintOutput is null.")
                } catch (e: IOException) {
                    e.printStackTrace()
                    callback?.onWriteFailed(e.message)
                } finally {
                    pdfDocument.close()
                }
            }
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal,
                layoutResultCallback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal.isCanceled) {
                    layoutResultCallback?.onLayoutCancelled()
                    return
                }
                val pdfDocumentInfo = PrintDocumentInfo.Builder("qr_code.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()

                layoutResultCallback?.onLayoutFinished(pdfDocumentInfo, true)
            }
        }

        val printAttributes = PrintAttributes.Builder().build()
        printManager.print("QR Code Print", printAdapter, printAttributes)
    }
}