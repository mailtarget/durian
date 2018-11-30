package co.mailtarget.durian

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.util.ArrayList
import java.util.regex.Pattern

/**
 *
 * @author masasdani
 * @since 4/6/17
 */
class DocumentCleaner
constructor(
        var options: ArrayList<Options>
) {

    private val regexRemoveNodes: String = "^side$|combx|retweet|menucontainer|navbar|^comment$|^commentContent$|^comment-body$|PopularQuestions|contact|foot|footer|Footer|footnote|cnn_strycaptiontxt|links|meta$|scroll|shoutbox|sponsor" +
            "|tags|socialnetworking|socialNetworking|cnnStryHghLght|cnn_stryspcvbx|^inset$|pagetools|post-attributes|welcome_form|contentTools2|the_answers" +
            "|communitypromo|runaroundLeft|^subscribe$|vcard|articleheadings|^date$|^print$|popup|tools|socialtools|byline|konafilter|KonaFilter|breadcrumb|^fn$|wp-caption-text|^column c160 left mb max$|^FL$" +
            "|^job_inner_tab_content$|^newsItem newsMagazine$|^newsItem newsOnline$|^float$|^mod-featured-title$|^below$|^quotePeekContainer$" +
            "|^header|header$|^menu|.*trending.*|^ads|_ad$|_ads$|^ad-|promo$|^promo|^survey|^related|related$|^login|login$|^register|register$|^signup|signup$|^search|search$|^notice|^notif|^action|^form" +
            "|^sharing|^share|sharing$|share$|back-to-top|^nav|control|relatedposts|.*_related|switch|^btn|sidebar|bottom|komentar|newsmore|button|sosmed|bacajuga|topiksisip|banner" +
            "|dtk-comment|clearfik|newstag|right_det"

    private val queryNaughtyIDs: String = "[id~=($regexRemoveNodes)]"
    private val queryNaughtyClasses: String = "[class~=($regexRemoveNodes)]"
    private val queryNaughtyNames: String = "[name~=($regexRemoveNodes)]"

    private val divToPElementsPattern = Pattern.compile("<(a|blockquote|dl|div|img|ol|p|pre|table|ul)")
    private val captionPattern = Pattern.compile("^caption$")
    private val googlePattern = Pattern.compile(" google ")
    private val entriesPattern = Pattern.compile("^[^entry-]more.*$")
    private val facebookPattern = Pattern.compile("[^-]facebook")
    private val twitterPattern = Pattern.compile("[^-]twitter")

    private val REPLACE_BRS = "(<br[^>]*>[ \n\r\t]*){2,}".toRegex()

    private val optionDefault = setOf(
            Options.CLEAN_HEADER,
            Options.CLEAN_FOOTER,
            Options.CLEAN_FORM,
            Options.CLEAN_BAD_TAGS,
            Options.CLEAN_FOOTER,
            Options.FONT_TO_SPAN,
            Options.CLEAN_DROP_CAPS,
            Options.CLEAN_SCRIPT_AND_STYLES
    )

    constructor() : this(arrayListOf())

    init {
        options.addAll(optionDefault)
    }

    fun clean(document: Document): Document {
        val docToClean = document

        if(options.contains(Options.CLEAN_HEADER)) cleanHeader(docToClean)
        if(options.contains(Options.CLEAN_FOOTER)) cleanFooter(docToClean)
        if(options.contains(Options.CLEAN_FORM)) cleanForm(docToClean)
        if(options.contains(Options.CLEAN_BAD_TAGS)) {
            cleanBadTags(docToClean)
            removeNodesViaRegEx(docToClean, captionPattern)
            removeNodesViaRegEx(docToClean, googlePattern)
            removeNodesViaRegEx(docToClean, entriesPattern)
            removeNodesViaRegEx(docToClean, facebookPattern)
            removeNodesViaRegEx(docToClean, twitterPattern)
        }

        if(options.contains(Options.FONT_TO_SPAN)) fontToSpan(docToClean)

        if(options.contains(Options.CLEAN_DROP_CAPS)) cleanDropCaps(docToClean)
        if(options.contains(Options.CLEAN_SCRIPT_AND_STYLES)) cleanScriptAndStyles(docToClean)
        if(options.contains(Options.CLEAN_COMMENTS)) clearComments(docToClean)

        if(options.contains(Options.CLEAN_SPAN_IN_P)) cleanSpanInP(docToClean)

        if(options.contains(Options.CLEAN_HR)) cleanHr(docToClean)
        if(options.contains(Options.CLEAN_ASIDE)) cleanAside(docToClean)
        if(options.contains(Options.CLEAN_CODE)) cleanCode(docToClean)
        if(options.contains(Options.CLEAN_CLEARFIX)) cleanClearfix(docToClean)
        if(options.contains(Options.CLEAN_EMPTY_P)) cleanEmptyP(docToClean)
        if(options.contains(Options.CLEAN_EMPTY_H)) cleanEmptyH(docToClean)

        if(options.contains(Options.NOSCRIPT_TO_DIV)) noScriptToDiv(docToClean)
        if(options.contains(Options.DOUBLE_BRS_TO_P)) wrapDoubleBrsParentWithP(docToClean)
        if(options.contains(Options.DOUBLE_BRS_TO_P)) doubleBrsToP(docToClean)
        if(options.contains(Options.DIV_TO_P)) divToP(docToClean)
        if(options.contains(Options.CLEAN_EM_TAGS)) cleanEmTags(docToClean)

        return docToClean
    }

    private fun fontToSpan(docToClean: Document) {
        val fonts = docToClean.getElementsByTag("font")
        for (font in fonts) {
            changeElementTag(docToClean, font, "span")
        }
    }

    private fun doubleBrsToP(docToClean: Document) {
        val doubleBrs = docToClean.select("br + br")
        for (br in doubleBrs) {
            // we hope that there's a 'p' up there....
            val parents = br.parents()
            var parent: Element? = parents.firstOrNull { it.tag().name == "p" }
            if (parent == null) {
                parent = br.parent()
                parent!!.wrap("<p></p>")
            }
            // now it's safe to make the change.
            var inner = parent.html()
            if (!inner.startsWith("<p>")) {
                inner = "<p>" + inner
            }
            inner = inner.replace(REPLACE_BRS, "</p><p>")
            parent.html(inner)
        }
    }

    private fun wrapDoubleBrsParentWithP(docToClean: Document) {
        val doubleBrs = docToClean.select("br + br")
        for (br in doubleBrs) {
            // we hope that there's a 'p' up there....
            val parents = br.parents()
            var parent: Element? = parents.firstOrNull { it.tag().name == "p" }
            if (parent == null) {
                parent = br.parent()
                parent!!.wrap("<p></p>")
            }
            // now it's safe to make the change.
            val inner = parent.html()
            parent.html(inner)
        }
    }

    private fun clearComments(docToClean: Document) {
        val childNodes = docToClean.childNodes()
        for (node in childNodes) {
            cleanComments(node)
        }
    }

    private fun noScriptToDiv(docToClean: Document) {
        val noScripts = docToClean.getElementsByTag("noscript")
        for (noScript in noScripts) {
            changeElementTag(docToClean, noScript, "div")
        }
    }

    private fun changeElementTag(docToClean: Document, e: Element, newTag: String): Element {
        val newElement = docToClean.createElement(newTag)
        /* JSoup gives us the live child list, so we need to make a copy. */
        val copyOfChildNodeList = ArrayList<Node>()
        copyOfChildNodeList.addAll(e.childNodes())
        for (n in copyOfChildNodeList) {
            n.remove()
            newElement.appendChild(n)
        }
        e.replaceWith(newElement)
        return e
    }

    private fun cleanComments(node: Node) {
        var i = 0
        while (i < node.childNodes().size) {
            val child = node.childNode(i)
            if (child.nodeName() == "#comment") {
                child.remove()
            } else {
                cleanComments(child)
                i++
            }
        }
    }

    private fun cleanEmptyP(docToClean: Document) {
        val paras = docToClean.select("p")
        paras.filter { para -> para.text().trim { it <= ' ' }.isEmpty() && para.childNodes().size == 0 }
                .forEach { it.remove() }
    }

    private fun cleanEmptyH(docToClean: Document) {
        val paras = docToClean.select("h2, h3, h4, h5, h6")
        paras.filter { it.text().isEmpty() && it.childNodes().size == 0 }.forEach { it.remove() }
    }

    /**
     * remove those css drop caps where they put the first letter in big text in
     * the 1st paragraph
     */
    private fun cleanDropCaps(document: Document) {
        val items = document.select("span[class~=(dropcap|drop_cap)]")
        for (item in items) {
            val tn = TextNode(item.text())
            item.replaceWith(tn)
        }
    }

    private fun cleanBadTags(document: Document) {
        // only select elements WITHIN the body to avoid removing the body itself
        val children = document.body().children()

        val naughtyList = children.select(queryNaughtyIDs)
        for (node in naughtyList) removeNode(node)

        val naughtyList3 = children.select(queryNaughtyClasses)
        for (node in naughtyList3) removeNode(node)

        // starmagazine puts shit on name tags instead of class or id
        val naughtyList5 = children.select(queryNaughtyNames)
        for (node in naughtyList5) removeNode(node)
    }

    private fun removeNode(node: Element?) {
        if (node == null || node.parent() == null) return
        if (node.getElementsByTag("h1").isEmpty() && node.select("p, br+br").isEmpty()) node.remove()
    }

    private fun removeNodesViaRegEx(document: Document, pattern: Pattern) {
        try {
            val naughtyList = document.getElementsByAttributeValueMatching("id", pattern)
            for (node in naughtyList) {
                removeNode(node)
            }
            val naughtyList3 = document.getElementsByAttributeValueMatching("class", pattern)
            for (node in naughtyList3) {
                removeNode(node)
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }

    }

    private fun cleanScriptAndStyles(document: Document) {
        val scripts = document.getElementsByTag("script")
        for (item in scripts) {
            item.remove()
        }
        val styles = document.getElementsByTag("style")
        for (style in styles) {
            style.remove()
        }
        val stylesheets = document.select("link[rel='stylesheet']")
        stylesheets.remove()
    }

    private fun divToP(document: Document) {
        val divs = document.getElementsByTag("div")
        for (div in divs) {
            try {
                val divToPElementsMatcher = divToPElementsPattern.matcher(div.html().toLowerCase())
                if (!divToPElementsMatcher.find()) {
                    replaceElementsWithPara(document, div)
                } else {
                    val replaceNodes = getReplacementNodes(document, div)
                    for (child in div.children()) {
                        child.remove()
                    }
                    for (node in replaceNodes) {
                        try {
                            div.appendChild(node)
                        } catch (ignored: Exception) {

                        }
                    }
                }
            } catch (ignored: NullPointerException) {

            }
        }
    }

    private fun getReplacementNodes(document: Document, div: Element): ArrayList<Node> {
        val replacementText = StringBuilder()
        val nodesToReturn = ArrayList<Node>()
        val nodesToRemove = ArrayList<Node>()

        for (kid in div.childNodes()) {
            if (kid.nodeName() == "p" && replacementText.isNotEmpty()) {
                // flush the buffer of text
                val newNode = getFlushedBuffer(replacementText, document)
                nodesToReturn.add(newNode)
                replacementText.setLength(0)
                if (kid is Element) {
                    nodesToReturn.add(kid)
                }
            } else if (kid.nodeName() == "#text") {
                val kidTextNode = kid as TextNode
                val kidText = kidTextNode.attr("text")
                if (kidText.isEmpty())
                    continue

                // clean up text from tabs and newlines
                var replaceText = kidText.replace("\n".toRegex(), "\n\n")
                replaceText = replaceText.replace("\t".toRegex(), "")
                replaceText = replaceText.replace("^\\s+$".toRegex(), "")

                if (replaceText.trim().length > 1) {
                    var previousSiblingNode: Node? = kidTextNode.previousSibling()
                    while (previousSiblingNode != null
                            && previousSiblingNode.nodeName() == "a"
                            && previousSiblingNode.attr("grv-usedalready") != "yes") {
                        replacementText.append(" ").append(previousSiblingNode.outerHtml()).append(" ")
                        nodesToRemove.add(previousSiblingNode)
                        previousSiblingNode.attr("grv-usedalready", "yes")
                        if (previousSiblingNode.previousSibling() != null) {
                            previousSiblingNode = previousSiblingNode.previousSibling()
                        } else {
                            previousSiblingNode = null
                        }
                    }
                    // add the text of the node
                    replacementText.append(replaceText)
                    // check the next set of links that might be after text (see
                    // businessinsider2.txt)
                    var nextSiblingNode: Node? = kidTextNode.nextSibling()
                    while (nextSiblingNode != null
                            && nextSiblingNode.nodeName() == "a"
                            && nextSiblingNode.attr("grv-usedalready") != "yes") {
                        replacementText.append(" ").append(nextSiblingNode.outerHtml()).append(" ")
                        nodesToRemove.add(nextSiblingNode)
                        nextSiblingNode.attr("grv-usedalready", "yes")
                        if (nextSiblingNode.nextSibling() != null) {
                            nextSiblingNode = nextSiblingNode.nextSibling()
                        } else {
                            nextSiblingNode = null
                        }
                    }
                }
                nodesToRemove.add(kid)
            } else {
                nodesToReturn.add(kid)
            }
        }
        // flush out anything still remaining
        if (replacementText.isNotEmpty()) {
            val newNode = getFlushedBuffer(replacementText, document)
            nodesToReturn.add(newNode)
            replacementText.setLength(0)
        }
        for (node in nodesToRemove) {
            node.remove()
        }

        return nodesToReturn

    }

    private fun getFlushedBuffer(replacementText: StringBuilder, document: Document): Element {
        val bufferedText = replacementText.toString()
        val newDoc = Document(document.baseUri())
        val newPara = newDoc.createElement("p")
        newPara.html(bufferedText)
        return newPara

    }

    private fun replaceElementsWithPara(doc: Document, div: Element) {
        val newDoc = Document(doc.baseUri())
        val newNode = newDoc.createElement("p")
        newNode.append(div.html())
        div.replaceWith(newNode)

    }

    private fun cleanSpanInP(document: Document) {
        val span = document.getElementsByTag("span")
        for (item in span) {
            if (item.parent().nodeName() == "p") {
                val tn = TextNode(item.text())
                item.replaceWith(tn)
            }
        }
    }

    private fun cleanEmTags(document: Document) {
        val ems = document.getElementsByTag("em")
        for (node in ems) {
            // replace the node with a div node
            val images = node.getElementsByTag("img")
            if (images.size != 0) {
                continue
            }
            val tn = TextNode(node.text())
            node.replaceWith(tn)
        }
    }

    private fun cleanHeader(document: Document) {
        val elements = document.getElementsByTag("header")
        for (node in elements) {
            node.remove()
        }
    }

    private fun cleanForm(document: Document) {
        val elements = document.getElementsByTag("form")
        for (node in elements) {
            node.remove()
        }
    }

    private fun cleanFooter(document: Document) {
        val elements = document.getElementsByTag("footer")
        for (node in elements) {
            node.remove()
        }
    }

    private fun cleanHr(document: Document) {
        val elements = document.getElementsByTag("hr")
        for (node in elements) {
            node.remove()
        }
    }

    private fun cleanAside(document: Document) {
        val elements = document.getElementsByTag("aside")
        for (node in elements) {
            node.remove()
        }
    }

    private fun cleanCode(document: Document) {
        val elements = document.getElementsByTag("code")
        for (node in elements) {
            node.remove()
        }
    }

    private fun cleanClearfix(document: Document) {
        val elements = document.select("div .clearfix")
        elements.filter { it.text().isEmpty() }.forEach { it.remove() }
    }

    enum class Options {
        CLEAN_COMMENTS,
        CLEAN_EMPTY_P,
        CLEAN_EMPTY_H,
        CLEAN_DROP_CAPS,
        CLEAN_BAD_TAGS,
        CLEAN_SCRIPT_AND_STYLES,
        CLEAN_SPAN_IN_P,
        CLEAN_HEADER,
        CLEAN_FORM,
        CLEAN_FOOTER,
        CLEAN_HR,
        CLEAN_ASIDE,
        CLEAN_CODE,
        CLEAN_CLEARFIX,
        CLEAN_EM_TAGS,
        FONT_TO_SPAN,
        DOUBLE_BRS_TO_P,
        NOSCRIPT_TO_DIV,
        DIV_TO_P,
    }

}