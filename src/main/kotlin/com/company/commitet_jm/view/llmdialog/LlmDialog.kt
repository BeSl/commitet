package com.company.commitet_jm.view.llmdialog


import com.company.commitet_jm.service.document.DocumentParser
import com.company.commitet_jm.view.filecommit.FileCommitDetailView
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.router.Route
import io.jmix.core.FileRef
import io.jmix.core.FileStorage
import io.jmix.core.FileStorageLocator
import io.jmix.flowui.component.upload.FileStorageUploadField
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.view.*
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.reader.ExtractedTextFormatter
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.io.IOException
import java.util.List
import java.util.Map


@Route(value = "llm-dialog", layout = MainView::class)
@ViewController(id = "LlmDialog")
@ViewDescriptor(path = "llm-dialog.xml")
class LlmDialog : StandardView() {

    companion object {
        private  val log = LoggerFactory.getLogger(FileCommitDetailView::class.java)
    }

    @Autowired
    private lateinit var  fileStorageLocator: FileStorageLocator

    @ViewComponent
    private lateinit var dataField: FileStorageUploadField

    @Autowired
    private lateinit var   qstore: QdrantVectorStore

    @Subscribe(id = "insFileDBButton", subject = "clickListener")
    private fun onInsFileDBButtonClick(event: ClickEvent<JmixButton>) {
//        val fileRef: FileRef =  dataField.value
//        val fileStorage: FileStorage = fileStorageLocator.getDefault()
//        try {
//            fileStorage.openStream(fileRef).use { inputStream ->
//                val parser = DocumentParser()
//                val extractedText = parser.extractText(inputStream)
//                var documents = parser.lazyChunkDocuments(extractedText, 500, 50, fileRef.storageName)
                var documents = getDocsFromPdfWithCatalog()
                qstore.add(documents)
//            }
//        } catch (e: IOException) {
//            // по желанию: логировать ошибку
////            throw RuntimeException("Ошибка при обработке файла ${fileRef.fileName}", e)
//        throw RuntimeException("Ошибка при обработке файла $e")
//        }


    }

    fun getDocsFromPdfWithCatalog():MutableList<Document>  {
//        val inputStream = File("D:/testPDF/docTest.pdf").inputStream()
        var pdfReader = ParagraphPdfDocumentReader(
            "classpath:/docTest.pdf",
            PdfDocumentReaderConfig.builder()
                .withPageTopMargin(0)
                .withPageExtractedTextFormatter(
                    ExtractedTextFormatter.builder()
                    .withNumberOfTopTextLinesToDelete(0)
                    .build())
                .withPagesPerDocument(1)
                .build());

        return pdfReader.read()
    }
}