package net.htmlparser.jericho;

import org.junit.Test;
import static org.junit.Assert.*;

public class CharacterReferenceTest {

	@Test public void testDecode() {
		assertEquals("b&b",CharacterReference.decode("b&amp;b")); // decode character entity reference with codepoint <= U+00FF
		assertEquals("b&b",CharacterReference.decode("b&#38;b")); // decode decimal numeric character reference with codepoint <= U+00FF
		assertEquals("b&b",CharacterReference.decode("b&#x26;b")); // decode hexadecimal numeric character reference with codepoint <= U+00FF
		assertEquals("x\u20acx",CharacterReference.decode("x&euro;x")); // decode character entity reference with codepoint > U+00FF
		assertEquals("x\u20acx",CharacterReference.decode("x&#8364;x")); // decode decimal numeric character reference with codepoint > U+00FF
		assertEquals("x\u20acx",CharacterReference.decode("x&#x20ac;x")); // decode hexadecimal numeric character reference with codepoint > U+00FF
	}

	@Test public void testDecodeCollapseWhitespace() {
		assertEquals("b & b",CharacterReference.decodeCollapseWhiteSpace("  b\n  &amp; \t b  ")); // convert all internal whitespace to a single space and trim front and back
		assertEquals("a  b   c   d",CharacterReference.decodeCollapseWhiteSpace("  a&nbsp; b &nbsp; c  &nbsp;  d")); // don't collapse non-breaking spaces, same as it would be rendered in a browser
		assertEquals(" X   ",CharacterReference.decodeCollapseWhiteSpace("&nbsp;X&nbsp; &nbsp;   ")); // non-breaking spaces are NOT trimmed from front or back, same as it would be rendered in a browser
		Config.ConvertNonBreakingSpaces=false;
		assertEquals("a\u00a0 b \u00a0 c \u00a0 d",CharacterReference.decodeCollapseWhiteSpace("  a&nbsp; b &nbsp; c  &nbsp;  d")); // Don't convert non-breaking spaces to spaces when Config.ConvertNonBreakingSpaces=false. Decode them as the actual non-breaking space character U+00A0.
		Config.ConvertNonBreakingSpaces=true;
	}
	
	@Test public void testDecodeUnterminated() {

		assertEquals("b&ampb",CharacterReference.decode("b&ampb")); // DO NOT decode unterminated character entity reference followed by an alphabetic character
		assertEquals("b& b",CharacterReference.decode("b&amp b")); // decode unterminated character entity reference followed by an NON-alphabetic character
		assertEquals("b&b",CharacterReference.decode("b&#38b")); // decode unterminated decimal numeric character reference followed by an alphabetic character
		assertEquals("b&x",CharacterReference.decode("b&#x26x",true)); // decode hexadecimal numeric character reference followed by an alphabetic character (only if insideAttributeValue=true with default configuration)

		// DEFAULT CONFIGURATION (Config.CurrentCompatibilityMode=Config.CompatibilityMode.IE):

		assertEquals("x& x",CharacterReference.decode("x&amp x",false)); // decode unterminated character entity reference with codepoint <= U+00FF with insideAttributeValue=false
		assertEquals("x& x",CharacterReference.decode("x&#38 x",false)); // decode unterminated decimal numeric character reference with codepoint <= U+00FF with insideAttributeValue=false
		assertEquals("x&#x26 x",CharacterReference.decode("x&#x26 x",false)); // DO NOT decode unterminated hexadecimal numeric character reference with codepoint <= U+00FF with insideAttributeValue=false

		assertEquals("x&euro x",CharacterReference.decode("x&euro x",false)); // DO NOT decode unterminated character entity reference with codepoint > U+00FF with insideAttributeValue=false
		assertEquals("x\u20ac x",CharacterReference.decode("x&#8364 x",false)); // decode unterminated decimal numeric character reference with codepoint > U+00FF with insideAttributeValue=false
		assertEquals("x&#x20ac x",CharacterReference.decode("x&#x20ac x",false)); // DO NOT decode unterminated hexadecimal numeric character reference with codepoint > U+00FF with insideAttributeValue=false

		assertEquals("x& x",CharacterReference.decode("x&amp x",true)); // decode unterminated character entity reference with codepoint <= U+00FF with insideAttributeValue=false
		assertEquals("x& x",CharacterReference.decode("x&#38 x",true)); // decode unterminated decimal numeric character reference with codepoint <= U+00FF with insideAttributeValue=false
		assertEquals("x& x",CharacterReference.decode("x&#x26 x",true)); // decode unterminated hexadecimal numeric character reference with codepoint <= U+00FF with insideAttributeValue=false

		assertEquals("x&euro x",CharacterReference.decode("x&euro x",true)); // DO NOT decode unterminated character entity reference with codepoint > U+00FF with insideAttributeValue=false
		assertEquals("x\u20ac x",CharacterReference.decode("x&#8364 x",true)); // decode unterminated decimal numeric character reference with codepoint > U+00FF with insideAttributeValue=false
		assertEquals("x\u20ac x",CharacterReference.decode("x&#x20ac x",true)); // decode unterminated hexadecimal numeric character reference with codepoint > U+00FF with insideAttributeValue=false
		
	}

	@Test public void testDecodeAttribute() {
		// demonstrates rules for decoding inside attribute value with default configuration (Config.CompatibilityMode.IE):
		// - unterminated &euro is not decoded as it has codepoint >= U+00FF
		// - unterminated &lt is not decoded as it is followed by an alphabetic character
		// - unterminated &gt is decoded as it is has codepoint < U+00FF and is not followed by an alphabetic character
		Source source=new Source("<a href=\"test?a=1&amp;b=2&c=3&euro=4&d=&ltx&gt&e=5\">test</a>");
		StartTag startTag=source.getFirstStartTag(HTMLElementName.A);
		String href=startTag.getAttributeValue("href");
		assertEquals("test?a=1&b=2&c=3&euro=4&d=&ltx>&e=5",href);
	}

	@Test public void testEncode() {
		assertNull(CharacterReference.encode(null,true));
		assertEquals("a&amp;b",CharacterReference.encode("a&b",true));
		assertEquals("&lt;a&gt;",CharacterReference.encode("<a>",true));
		assertEquals("&quot;a&quot;",CharacterReference.encode("\"a\"",true));
		assertEquals("\"a\"",CharacterReference.encode("\"a\"",false));
		assertEquals("'a'",CharacterReference.encode("'a'",true));
		Config.IsApostropheEncoded=true;
		assertEquals("&#39;a&#39;",CharacterReference.encode("'a'",true));
		assertEquals("\u20ac",CharacterReference.encode("\u20ac",true));
		Config.CurrentCharacterReferenceEncodingBehaviour=Config.LEGACY_CHARACTER_REFERENCE_ENCODING_BEHAVIOUR;
		assertEquals("&euro;",CharacterReference.encode("\u20ac",true));
		assertEquals("&#8356;",CharacterReference.encode("\u20a4",true));
		assertEquals("&#x20a4;",NumericCharacterReference.encodeHexadecimal("\u20a4"));
	}
	
}
