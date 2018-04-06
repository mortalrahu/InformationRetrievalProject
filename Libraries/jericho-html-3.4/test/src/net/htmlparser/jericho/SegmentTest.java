package net.htmlparser.jericho;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class SegmentTest {
	private static final String sourceUrlString="file:test/data/SegmentTest.html";

	@Test public void test() throws Exception {
		Source source=new Source(new URL(sourceUrlString));
		test(source);
		source.fullSequentialParse();
		test(source); // test all functions again using cached results
	}
		
	private void test(Source source) throws Exception {
		assertEquals("<HTML>",source.getFirstStartTag().toString());
		assertEquals("SegmentTest",source.getFirstElement("title").getContent().toString());
		assertEquals("document keywords",source.getFirstStartTag("name","keywords",false).getAttributeValue("content"));
		assertEquals("<!--<p>commented p</p>-->",source.getFirstStartTag(StartTagType.COMMENT).toString());
		assertEquals(0,source.getAllStartTags("di").size()); // make sure searching for "di" doesn't find the "div" tag
		assertEquals(1,source.getAllStartTags("div ").size()); // extra characters after the start tag name are allowed!
		
		Segment outerDiv=source.getElementById("OuterDiv");
		List<Element> elements=outerDiv.getAllElements();
		assertEquals(4,elements.size()); // outerDiv itself plus 3 contained elements
		assertEquals(StartTagType.COMMENT,elements.get(2).getStartTag().getTagType());
		assertEquals(1,outerDiv.getAllElements(StartTagType.COMMENT).size());
		assertEquals(2,outerDiv.getAllElements(HTMLElementName.P).size());
		
		Segment testSegment=new Segment(source,outerDiv.getBegin(),source.getElementById("p2").getStartTag().getEnd()+1); // this segment ends in the middle of the content of p2.
		assertEquals(4,testSegment.getAllStartTags().size()); // outerDiv start tag plus 3 contained start tags
		assertEquals(2,testSegment.getAllElements().size()); // only p1 and comment, as outerDiv and p2 are not enclosed by testSegment
		assertNull(source.getFirstStartTag(StartTagType.COMMENT).getFirstStartTag(HTMLElementName.P));
		assertNull(testSegment.getFirstElement("id","p2",true));
		
		assertEquals(0,source.getAllElementsByClass("de").size());
		List<Element> defElements=source.getAllElementsByClass("def");
		assertEquals(4,defElements.size());
		assertEquals("p4",defElements.get(0).getContent().toString());
		assertEquals("p5",defElements.get(1).getContent().toString());
		assertEquals("p6",defElements.get(2).getContent().toString());
		assertEquals("p7",defElements.get(3).getContent().toString());
		
		List<StartTag> startTags=source.getAllStartTags(StartTagType.SERVER_COMMON);
		assertEquals(3,startTags.size()); // normal server tag, server variable and jsp page directive
		assertEquals("%",startTags.get(0).getName()); // shouldn't include any content from server tag except the prefix
		assertEquals("%",startTags.get(1).getName()); // shouldn't include any content from server tag except the prefix, not even the = character
		assertEquals("%",startTags.get(2).getName()); // shouldn't include any content from server tag except the prefix, not even the @page directive
		StartTag startTag=source.getFirstStartTag(StartTagType.SERVER_COMMON_COMMENT);
		assertNotNull(startTag);
		assertEquals("%--",startTag.getName()); // shouldn't include any content except the prefix
		startTag=source.getFirstStartTag(StartTagType.SERVER_COMMON_ESCAPED);
		assertNotNull(startTag);
		assertEquals("\\%",startTag.getName()); // shouldn't include any content except the prefix
		startTags=source.getAllStartTags(StartTagType.UNREGISTERED);
		assertEquals(1,startTags.size()); // should find the <#unregistered tag type#>
		assertEquals("",startTags.get(0).getName()); // shouldn't include any content except the prefix which is empty for unregistered tag types
	
		startTags=source.getAllStartTags("jsp:"); // search for tags in the jsp namespace
		assertEquals(1,startTags.size());
		assertEquals("jsp:directive.page",startTags.get(0).getName());
		startTags=source.getAllStartTags("jsp");
		assertEquals(0,startTags.size()); // doesn't find the jsp: directive because the search doesn't end with the ':' namespace character
		startTags=source.getAllStartTags("jsp:directive.page");
		assertEquals(1,startTags.size());
		startTags=source.getAllStartTags("jsp:directive.");
		assertEquals(0,startTags.size()); // '.' is a valid XML name character so can't do partial searches ending in that character.
		startTags=source.getAllStartTags("%");
		assertEquals(0,startTags.size()); // searching by name only returns start tags of type NORMAL so doesn't return any server tags
		startTags=source.getAllStartTags("#");
		assertEquals(1,startTags.size()); // searching by name can also return UNREGISTERED tag types if the search name is not a valid XML name
		assertEquals("",startTags.get(0).getName()); // the tag name of UNREGISTERED tags is always empty, even though it was found using a name search. 
		startTags=source.getAllStartTags("#unreg");
		assertEquals(1,startTags.size()); // searching UNREGISTERED tag types with incomplete names also works
		startTag=source.getNextStartTag(0,"#unregistered",StartTagType.UNREGISTERED);
		assertNotNull(startTag);
		startTag=source.getNextStartTag(0,"%",StartTagType.SERVER_COMMON);
		assertNotNull(startTag);
		assertEquals("server tag",startTag.getTagContent().toString());
		startTag=source.getNextStartTag(0,"%@page",StartTagType.SERVER_COMMON);
		assertNotNull(startTag);
		assertEquals("%",startTag.getName()); // the tag name of a SERVER_COMMON tag is always "%", even if it was found using a name search.
		startTag=source.getNextStartTag(0,"\\",StartTagType.SERVER_COMMON_ESCAPED);
		assertNotNull(startTag);
		assertEquals("\\%",startTag.getName()); // the tag name of a SERVER_COMMON_ESCAPED tag is always "\%"
	}
}
