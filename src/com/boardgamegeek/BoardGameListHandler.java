package com.boardgamegeek;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class BoardGameListHandler extends DefaultHandler {

	private List<BoardGame> boardGameList;
	private BoardGame boardGame;
	private StringBuilder currentElement;

	// returns object after parsing
	public List<BoardGame> getBoardGameList() {
		return boardGameList;
	}

	@Override
	public void startDocument() throws SAXException {
		boardGameList = new ArrayList<BoardGame>();
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {

		currentElement = new StringBuilder();

		if (localName.equals("boardgame")) {
			boardGame = new BoardGame();
			boardGame.setGameId(atts.getValue("objectid"));
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		if (localName == "name") {
			boardGame.setName(currentElement.toString());
		} else if (localName == "yearpublished") {
			try {
				boardGame.setYearPublished(new Integer(currentElement
						.toString()));
			} catch (NumberFormatException ex) {
				boardGame.setYearPublished(0);
			}
		}
		else
		if (localName.equals("boardgame")) {
			boardGameList.add(boardGame);
		}
	}

	@Override
	public void characters(char ch[], int start, int length) {
		currentElement.append(ch, start, length);
	}
}