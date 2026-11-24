package com.example.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.model.FilledFormRecord
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PdfPrintAndShareHelper {

    fun printForm(context: Context, record: FilledFormRecord) {
        val pdfFile = ClinicalPdfGenerator.generatePdfFile(context, record)
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "${record.templateName} - ${record.patientMrdNo}"
        val adapter = PdfFilePrintDocumentAdapter(pdfFile, "${record.templateName}_${record.patientMrdNo}.pdf")
        val attrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        printManager.print(jobName, adapter, attrs)
    }

    fun sharePdf(context: Context, record: FilledFormRecord) {
        val pdfFile = ClinicalPdfGenerator.generatePdfFile(context, record)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${record.templateName} — ${record.patientName} (${record.patientMrdNo})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Clinical Form PDF"))
    }

    fun writePdfToUri(context: Context, record: FilledFormRecord, destinationUri: Uri): Result<Unit> {
        return try {
            val pdfFile = ClinicalPdfGenerator.generatePdfFile(context, record)
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                FileInputStream(pdfFile).use { input ->
                    input.copyTo(out)
                }
            } ?: error("Unable to open destination PDF file.")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun suggestedPdfFilename(record: FilledFormRecord): String {
        val tpl = record.templateName.replace(Regex("[^a-zA-Z0-9]+"), "_").trim('_').take(28)
        val mrd = record.patientMrdNo.replace(Regex("[^a-zA-Z0-9]+"), "_").trim('_').take(20)
        return "${tpl}_${mrd}.pdf"
    }

    private class PdfFilePrintDocumentAdapter(
        private val file: File,
        private val docName: String
    ) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder(docName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            try {
                FileInputStream(file).use { input ->
                    FileOutputStream(destination.fileDescriptor).use { output ->
                        input.copyTo(output)
                    }
                }
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                } else {
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }
            } catch (e: Exception) {
                callback.onWriteFailed(e.localizedMessage ?: "PDF write error")
            }
        }
    }
}
