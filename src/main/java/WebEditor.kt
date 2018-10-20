import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import javafx.application.Application
import javafx.collections.FXCollections
import javafx.embed.swing.SwingFXUtils
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyEvent
import javafx.scene.web.WebView
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.net.URL
import java.util.*
import javax.imageio.ImageIO

/**
 *
 */
data class OverlayAddonData(var url: String,
                        var x: Int,
                        var y: Int,
                        var width: Double,
                        var height: Double,
                        var name: String,
                        var favicon: String,
                        var description: String,
                        var abbreviation: String
                        )
{
    override fun toString(): String
    {
        return name;
    }
}

data class WebAddonData(var url: String,
                        var x_dimension: Double,
                        var y_dimension: Double,
                        var width: Double,
                        var height: Double,
                        var name: String)
{
    fun convert(): OverlayAddonData
    {
        EditorController.increment()
        return OverlayAddonData(url, EditorController._x, EditorController._y, width, height, name, "", "", "")
    }
}

class EditorController : Initializable
{
    companion object
    {
        lateinit var active: OverlayAddonData
        lateinit var stage: Stage
        var _x = -1
        var _y = 0
        var webStage: Stage = Stage()

        fun increment()
        {
            when (_x)
            {
                10 -> {_x = 0; _y++;}
                else -> _x++
            }
        }
    }

    fun githubList(): Array<OverlayAddonData>
    {
        val url = URL("https://raw.githubusercontent.com/POE-Addon-Launcher/Curated-Repo/master/websites.json")

        var objectMapper = ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
                .registerModule(KotlinModule())
        return objectMapper.readValue(url, Array<OverlayAddonData>::class.java)
    }

    override fun initialize(location: URL?, resources: ResourceBundle?)
    {
        createUI(Stage(), 855.0, 855.0)
        checkForNewSelection()
        //checkForWebengineError()
    }

    fun checkForWebengineError()
    {
        web.engine.loadWorker.stateProperty().addListener{ _, _, _ ->
            var exception = web.engine.loadWorker.exceptionProperty()
            when (exception.value)
            {
                null -> stage.width = 1100.0
                else -> {stage.width = 625.0; println(web.engine.loadWorker.exceptionProperty());}
            }

        }
    }

    fun populateList(a: Array<OverlayAddonData>)
    {
        val z = FXCollections.observableArrayList<OverlayAddonData>()
        z.forEach { println(it.name) }
        z.addAll(a)
        GlobalScope.launch(Dispatchers.Main) { listSites!!.items = z }
    }

    var cached: OverlayAddonData? = null

    fun checkForNewSelection()
    {
            GlobalScope.launch {
                while (true)
                {
                    if (cached == null && listSites.selectionModel.selectedItem != null)
                    {
                        cached = listSites.selectionModel.selectedItem
                        GlobalScope.launch(Dispatchers.Main) { setData(listSites.selectionModel.selectedItem) }
                    }
                    else
                    {
                        if (cached != listSites.selectionModel.selectedItem)
                        {
                            cached = listSites.selectionModel.selectedItem
                            GlobalScope.launch(Dispatchers.Main) { setData(listSites.selectionModel.selectedItem) }
                        }
                    }
                    delay(100L)
                }

            }
    }

    fun setData(data: OverlayAddonData)
    {
        println(data.name)
        url.text = data.url
        x.text = data.x.toString()
        y.text = data.y.toString()
        w.text = data.width.toString()
        h.text = data.height.toString()
        name.text = data.name
        faviconURL.text = data.favicon
        description.text = data.description
        abrev.text = data.abbreviation
        faviconImg.image = setFavicon(data.favicon, data.name).image
        WebAddonController.url = data.url
        webStage.width = w.text.toDouble()
        webStage.height = h.text.toDouble()
    }

    val imageSize = 32

    fun setFavicon(favicon: String, name: String): ImageView
    {
        try
        {
            var image: Image = SwingFXUtils.toFXImage(ImageIO.read(URL(favicon)), null)
            var img = ImageView(image)
            img.fitHeight = imageSize.toDouble()
            img.fitWidth = imageSize.toDouble()
            return img
        }
        catch (e: Exception)
        {
            return createImageWithText(ImageCreator.nameSanitizer(name))
        }
        catch (e: IOException)
        {
            return createImageWithText(ImageCreator.nameSanitizer(name))
        }
    }

    fun createImageWithText(char: String): ImageView
    {
        val image = BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_RGB)
        // Fill it with some color.
        val randomColour = ImageCreator.createRandomColour()
        var intArray: IntArray = IntArray(imageSize * imageSize)
        for (i in 0 until imageSize)
            intArray[i] = randomColour

        image.setRGB(0,0,imageSize,imageSize,intArray,0,0)

        var base = Font("Arial", Font.BOLD, 24)
        image.graphics.font = ImageCreator.pickOptimalFontSize(image.createGraphics(), char, 64, 64, base)
        ImageCreator.drawString(image.createGraphics(), char, imageSize/2.0, imageSize/2.0, "center", "center")


        var fxImage: Image = SwingFXUtils.toFXImage(image, null)
        return ImageView(fxImage)
    }

    @FXML
    lateinit var listSites: ListView<OverlayAddonData>

    @FXML
    lateinit var url: TextField

    @FXML
    lateinit var x: TextField

    @FXML
    lateinit var y: TextField

    @FXML
    lateinit var w: TextField

    @FXML
    lateinit var h: TextField

    @FXML
    lateinit var name: TextField

    @FXML
    lateinit var faviconURL: TextField

    @FXML
    lateinit var description: TextArea

    @FXML
    lateinit var faviconImg: ImageView

    @FXML
    lateinit var abrev: TextField

    @FXML
    lateinit var bSave: Button

    @FXML
    lateinit var bNew: Button

    @FXML
    lateinit var web: WebView

    fun newEntry(event: ActionEvent)
    {
        increment()
        listSites.items.add(OverlayAddonData("", _x, _y, 855.0, 855.0, "NEW SITE", "", "", ""))
    }

    fun save(actionEvent: ActionEvent)
    {
        val f = File("${System.getenv("LOCALAPPDATA")}${File.separator}TEMP${File.separator}websites.json")

        if (f.exists())
            f.delete()


        var array: Array<OverlayAddonData?> = arrayOfNulls<OverlayAddonData>(listSites.items.size)

        for (c in 0 until listSites.items.size)
            array[c] = listSites.items[c]


        var objectMapper = ObjectMapper()
        objectMapper.writeValue(f, array)

        Runtime.getRuntime().exec("explorer ${System.getenv("LOCALAPPDATA")}${File.separator}TEMP")
    }

    fun delete(actionEvent: ActionEvent)
    {
        GlobalScope.launch(Dispatchers.Main) { listSites.items.remove(listSites.selectionModel.selectedItem) }
    }

    fun loadGithub(actionEvent: ActionEvent)
    {
        GlobalScope.launch(Dispatchers.Main) {
            val foo = githubList()
            delay(1000)
            populateList(foo)
            delay(1000)

        }
    }

    fun createUI(primaryStage: Stage, width: Double, height: Double)
    {
        webStage.close()
        var primaryStage = primaryStage
        primaryStage = Stage()
        val fxmlLoader = FXMLLoader()
        fxmlLoader.setController(WebAddonController())
        val root = fxmlLoader.load<Parent>(javaClass.getResource("WebAddonUI.fxml").openStream())
        primaryStage.initStyle(StageStyle.UTILITY)
        val scene = Scene(root, width, height)
        primaryStage.scene = scene
        primaryStage.isAlwaysOnTop = true
        webStage = primaryStage
        webStage.show()
    }

    fun load(actionEvent: ActionEvent)
    {

    }

    fun saveInfo()
    {
        GlobalScope.launch(Dispatchers.Main) {
            listSites.isDisable = true
            active = OverlayAddonData(url.text, x.text.toInt(), y.text.toInt(), w.text.toDouble(), h.text.toDouble(), name.text, faviconURL.text, description.text, abrev.text)
            listSites.items.remove(listSites.selectionModel.selectedItem)
            listSites.items.add(active)
            listSites.selectionModel.select(active)
            listSites.isDisable = false
        }
    }

    fun urlKeyReleased(keyEvent: KeyEvent)
    {
        GlobalScope.launch(Dispatchers.Main) { WebAddonController.url = url.text }
    }

    fun faviKeyReleased(keyEvent: KeyEvent)
    {
        GlobalScope.launch(Dispatchers.Main) { faviconImg.image = setFavicon(faviconURL.text, abrev.text).image }
    }

    fun onKeyReleaseName(keyEvent: KeyEvent)
    {
        GlobalScope.launch(Dispatchers.Main) { abrev.text = ImageCreator.nameSanitizer(name.text) }
    }

    fun sizeChange(actionEvent: KeyEvent)
    {
        GlobalScope.launch { webStage.height = h.text.toDouble(); webStage.width = w.text.toDouble() }
    }
}
class WebAddonController: Initializable
{
    companion object
    {
        var url: String = "https://www.reddit.com/r/pathofexile"
    }
    override fun initialize(location: URL?, resources: ResourceBundle?)
    {
        checkForWebengineError()
        checkForNewUrl()
    }

    fun checkForWebengineError()
    {
        webView.engine.loadWorker.stateProperty().addListener{ _, _, _ ->
            var exception = webView.engine.loadWorker.exceptionProperty()
            when (exception.value)
            {
                null -> "do nothing"
                else -> {println(webView.engine.loadWorker.exceptionProperty()); url = "https://google.com"}
            }

        }
    }

    fun checkForNewUrl()
    {
        GlobalScope.launch(Dispatchers.Main) {
            while (true)
            {
                delay(10L)
                if (url != webView.engine.location)
                {
                    webView.engine.load(url)
                }
            }
        }
    }

    @FXML
    lateinit var webView: WebView
}

class Editor : Application()
{
    override fun start(primaryStage: Stage?)
    {
        var primaryStage = primaryStage
        primaryStage = Stage()
        val fxmlLoader = FXMLLoader()
        val root = fxmlLoader.load<Parent>(javaClass.getResource("editor.fxml").openStream())
        primaryStage.title = "PAL: WebsiteJson Editor"
        val scene = Scene(root, 600.0, 400.0 )
        primaryStage.scene = scene
        primaryStage.isAlwaysOnTop = true
        EditorController.stage = primaryStage
        primaryStage.show()
    }
}

fun main(args: Array<String>)
{
    Application.launch(Editor::class.java, *args)
}

class ImageCreator
{
    companion object
    {
        fun drawString(g: Graphics, str: String, x: Double, y: Double, hAlign: String, vAlign: String)
        {

            var metrics = g.getFontMetrics();
            var dX = x.toInt()
            var dY = y.toInt()

            when (hAlign.toLowerCase())
            {
                "center" -> dX -= (metrics.getStringBounds(str, g).width /2.0).toInt()
                "right" -> dX -= (metrics.getStringBounds(str, g).width).toInt()
            }

            when (vAlign.toLowerCase())
            {
                "center" -> dY += (metrics.ascent / 2.0).toInt()
                "top" -> dY += metrics.ascent
            }
            g.drawString(str, dX, dY)
        }

        fun pickOptimalFontSize(g: Graphics2D, title: String, width: Int, height: Int, baseFont: Font): Font
        {
            lateinit var rect: Rectangle2D

            var fontSize = 32 //initial value
            var font: Font
            do
            {
                fontSize-=1;
                font = baseFont.deriveFont(fontSize);
                rect = getStringBoundsRectangle2D(g, title, font);
            }
            while (rect.getWidth() >= width || rect.getHeight() >= height)
            return font;
        }

        fun getStringBoundsRectangle2D(g: Graphics, title: String, font: Font): Rectangle2D
        {
            g.setFont(font)
            val fontMetrics = g.getFontMetrics()
            val rect = fontMetrics.getStringBounds(title, g)
            return rect
        }

        fun createRandomColour(): Int
        {
            val rng = Random()
            lateinit var hexR: String
            lateinit var hexG: String
            lateinit var hexB: String
            // Generates colours from ranges FF9aFF to FF00FF
            // Produces the best colours IMO, could be tweaked further.
            return rng.nextInt(16751359 - 16711935 )
        }

        fun nameSanitizer(text: String): String
        {
            var str = text
            str = str.replace("PoE", "")
            str = str.replace("POE", "")
            str = str.replace("poe", "")
            str = str.replace(",", "")
            str = str.replace(" ", "")
            str = str.replace(Regex("([a-z])"), "")
            str = str.replace("//", "/r/")
            println("New: $str Old: $text")

            when (true)
            {
                str.length <= 3 -> return str;
                else -> return str.substring(0,3)
            }
        }
    }

}