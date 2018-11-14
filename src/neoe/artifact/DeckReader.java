package neoe.artifact;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import neoe.util.Config;
import neoe.util.Etc;
import neoe.util.FileUtil;

public class DeckReader {

	public static void main(String[] args) throws Exception {
		System.out.println(new DeckReader().readFile(args.length == 0 ? "deck1.txt" : args[0]));
	}

	private String readFile(String fn) throws Exception {
		return readText(FileUtil.readString(new FileInputStream(fn), null), fn);
	}

	private String readText(String text, String fn) throws Exception {
		String[] lines = text.split("\n");
		int cat = 0;// hero, card, item
		initNameMap();
		List heroes = new ArrayList();
		List cards = new ArrayList();
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.length() == 0 || line.startsWith("#"))
				continue;
			if ("Heroes:".equals(line)) {
				cat = 0;
			} else if ("Main Deck:".equals(line)) {
				cat = 1;
			} else if ("Item Deck:".equals(line)) {
				cat = 2;
			} else {
				int p1 = line.indexOf(' ');
				int v = Integer.parseInt(line.substring(0, p1));
				String name = line.substring(p1 + 1).trim();
				Integer id = (Integer) nameMap[cat].get(name);
				if (id == null) {
					Etc.BEM("cannot find card for " + name);
				}
				if (cat == 0) {
					int cnt = heroes.size();
					int pos;
					if (cnt < 3) {
						pos = 1;
					} else {
						pos = cnt - 1;
					}
					heroes.add(new int[] { id, pos });
				} else {
					cards.add(new int[] { id, v });
				}
			}
		}
		String deckname = fn;
		int p1 = fn.lastIndexOf('.');
		if (p1 > 0) {
			deckname = fn.substring(0, p1);
		}
		if (Etc.isEmpty(deckname))
			deckname = "noname";
		return new DeckEncoder().encodeDeck(deckname, trans(heroes), trans(cards));
	}

	private static int[][] trans(List a) {
		int c = a.size();
		int[][] r = new int[c][];
		for (int i = 0; i < c; i++) {
			r[i] = (int[]) a.get(i);
		}
		return r;
	}

	static Map[] nameMap;

	private void initNameMap() throws Exception {
		if (nameMap != null)
			return;
		nameMap = new Map[] { new HashMap(), new HashMap(), new HashMap(), };
		Map cm = new CardSets().getCardMap();
		for (Object o : cm.values()) {
			Map c = (Map) o;
			int cat = getCat(c);
			if (cat == -1)
				continue;
			String name = Config.gets(c, "card_name.english");
			int id = Config.toInt(c.get("card_id"));
			if (nameMap[cat].containsKey(name)) {
				Etc.BEM(String.format("dup name %s (%s,%s)", name, nameMap[cat].get(name), id));
			} else {
				nameMap[cat].put(name, id);
			}
		}
		System.out.printf("namemap(%d,%d,%d)\n", nameMap[0].size(), nameMap[1].size(), nameMap[2].size());
	}

	private int getCat(Map c) {
		String type = (String) c.get("card_type");
		if (Etc.isEmpty(type))
			Etc.BEM("no card_type:" + c);
		if ("Ability".equals(type))
			return -1;
		if ("Hero".equals(type))
			return 0;
		if ("Item".equals(type))
			return 2;
		return 1;
	}

}
