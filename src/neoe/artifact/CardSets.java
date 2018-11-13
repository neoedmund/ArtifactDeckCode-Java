package neoe.artifact;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import neoe.util.Config;
import neoe.util.FileUtil;
import neoe.util.PyData;
import neoe.util.UrlUtil;

public class CardSets {

	public static void main(String[] args) throws Exception {
		System.out.println(new CardSets().getCardMap().size());
	}

	static String[] sets = { "00", "01" };
	Map m;

	public Map getCardMap() throws Exception {
		if (m != null)
			return m;
		m = new LinkedHashMap<>();
		for (String set : sets) {
			addSet(set);
		}
		return m;
	}

	public static void clearCacheFile() {
		for (String set : sets) {
			new File("card_set_" + set + ".json").delete();
		}
	}

	private void addSet(String set) throws Exception {
		if (loadFromJson(set)) {
			return;
		}
		fetchFromSite(set);
		if (!loadFromJson(set)) {
			System.out.println("cannot load set " + set);
		}
	}

	private void fetchFromSite(String set) throws Exception {
		String url1 = String.format("https://playartifact.com/cardset/%s/?language=CN-zh", set);
		String json = new UrlUtil().download(url1).getPage();
		Map m = (Map) PyData.parseAll(json, false, true);
		String url2 = "" + m.get("cdn_root") + m.get("url");
		System.out.println("redirect:" + url2);
		String json2 = new UrlUtil().download(url2).getPage();
		FileUtil.save(json2.getBytes("UTF8"), "card_set_" + set + ".json");
		System.out.println("saved set " + set);
	}

	private boolean loadFromJson(String set) throws Exception {
		File f = new File("card_set_" + set + ".json");
		if (!f.exists()) {
			return false;
		}
		Map s = (Map) PyData.parseAll(FileUtil.readString(new FileInputStream(f), null), false, true);
		List cs = (List) Config.get(s, "card_set.card_list");
		int cnt = 0;
		for (Object o : cs) {
			Map card = (Map) o;
			m.put(Config.toInt(card.get("card_id")), card);
			cnt++;
		}
		System.out.println("add set " + set + " cards cnt:" + cnt);
		return true;
	}

}
