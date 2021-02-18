import kieker.analysis.AnalysisController
import kieker.analysis.IAnalysisController
import kieker.analysis.plugin.filter.forward.TeeFilter
import kieker.analysis.plugin.reader.filesystem.FSReader
import kieker.common.configuration.Configuration
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.system.exitProcess

fun main(args: Array<String>)
{
    if (args.isEmpty())
    {
        println(
            "Invalid number of Arguments.\n" +
                    "Usage: Kiefer_ETL <path_to_kiefer_log_directory>"
        )
        exitProcess(1)
    }

    val logDirectory = args[0]

    // iterate through all TeaStore Logs
    val datFilesInDirectory = File(logDirectory)
        .walkTopDown()
        .maxDepth(1)
        .filter { file -> file.extension == "dat" }

    val regex = "-(\\d*)-".toRegex()
    for (file in datFilesInDirectory)
    {
        val dateFromFileNameAsString = regex.find(file.name)?.groupValues?.get(1)
        if (dateFromFileNameAsString.isNullOrEmpty())
        {
            println("Could not extract date from file ${file.name}. Skipping")
            continue
        }
        val dateFromFileName = LocalDate.parse(
            dateFromFileNameAsString,
            DateTimeFormatter.ofPattern("yyyyMMdd")
        )

        val analysisController: IAnalysisController = AnalysisController()

        // read logs from file system
        val readerConfig = Configuration()
        readerConfig.setProperty(
            TeaStoreLogFileReader.CONFIG_PROPERTY_NAME_FILE_NAME,
            file.path
        )
        val teaStoreLogFileReader = TeaStoreLogFileReader(readerConfig, analysisController)

        // filter operations of the servlets because they handle HTTP Requests from users
        val opFilterConfig = Configuration()
        opFilterConfig.setProperty(OperationSignatureFilter.CONFIG_PROPERTY_NAME_OPERATION, "servlet")
        val operationSignatureFilter = OperationSignatureFilter(opFilterConfig, analysisController)
        analysisController.connect(
            teaStoreLogFileReader,
            TeaStoreLogFileReader.OUTPUT_PORT_NAME_RECORDS,
            operationSignatureFilter,
            OperationSignatureFilter.INPUT_PORT_NAME_OPERATIONS
        )

        // convert events to GS command log format
        val converterFilter = GSCommandLogConverterFilter(Configuration(), analysisController)
        analysisController.connect(
            operationSignatureFilter,
            OperationSignatureFilter.OUTPUT_PORT_NAME_FILTERED_EVENTS,
            converterFilter,
            GSCommandLogConverterFilter.INPUT_PORT_NAME_RECORDS
        )

        // collect all events in a list
        val collectToListFilter = CollectToListFilter(Configuration(), analysisController)
        analysisController.connect(
            converterFilter,
            GSCommandLogConverterFilter.OUTPUT_PORT_NAME_GS_CMD_LOG_ENTRIES,
            collectToListFilter,
            CollectToListFilter.INPUT_PORT_NAME_EVENTS
        )

        // TODO Remove the TeeFilter
        val teeFilterConfig = Configuration()
        teeFilterConfig.setProperty(
            TeeFilter.CONFIG_PROPERTY_NAME_STREAM,
            TeeFilter.CONFIG_PROPERTY_VALUE_STREAM_STDOUT
        )
        val teeFilter = TeeFilter(teeFilterConfig, analysisController)
        analysisController.connect(
            collectToListFilter,
            CollectToListFilter.OUTPUT_PORT_NAME_RELAYED_EVENTS,
            teeFilter,
            TeeFilter.INPUT_PORT_NAME_EVENTS
        )

        // execute the pipeline
        analysisController.run()

        println("Count: ${collectToListFilter.internalList.size}")

        val sortedList = collectToListFilter.internalList
            .map { any -> any as GSCommandLogEntry }
            .sortedBy { entry -> entry.timestamp }

        // write the events in GS command log format to a new file
        FileOutputStream("${logDirectory}${File.separatorChar}teastore-cmd_$dateFromFileName.log")
            .bufferedWriter()
            .use { out ->
                sortedList.forEach { entry ->
                    out.write(entry.toString())
                    out.newLine()
                }
            }
    }
}