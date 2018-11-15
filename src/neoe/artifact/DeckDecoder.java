package neoe.artifact;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import neoe.util.Base64;
import neoe.util.Config;
import neoe.util.Etc;

/** this shows how to decoder the deck hash. Could use by further programs. */
public class DeckDecoder {

	public static void main(String[] args) throws Exception {
		new DeckDecoder()
				.run(args.length == 0 ? "ADCJWkTZX05uwGDCRV4XQGy3QGLmqUBg4GQJgGLGgO7AaABR3JlZW4vQmxhY2sgRXhhbXBsZQ__"
						: args[0]);
	}

	public void run(String code) throws Exception {
		System.out.println(code);
		String prefix = "ADC";
		if (!code.startsWith(prefix)) {
			Etc.BEM("should start with " + prefix);
		}
		byte[] bs = Base64.decode(code.substring(3).replace('-', '/').replace('_', '='));
		parseDeck(bs);
	}

	int CurrentVersion = 2;

	public Object[] parseDeck(byte[] bs) throws Exception {
		int i = 0;
		int cnt = bs.length;
		byte verAndHeroes = bs[i++];
		int ver = verAndHeroes >> 4;
		if (CurrentVersion != ver && ver != 1) {
			Etc.BEM("version not supported:" + ver);
		}
		byte checksum = bs[i++];
		int strLen = 0;
		if (ver > 1) {
			strLen = bs[i++];
		}
		int cardLen = cnt - strLen;

		byte csum = 0;
		for (int j = i; j < cardLen; j++) {
			csum += bs[j];
		}
		if (checksum != csum) {
			Etc.BEM(String.format("checksum not correct. %x!=%x", checksum, csum));
		}
		int[] numHeroes = { 0 };
		{
			int[] ii = { i };
			if (!ReadVarEncodedUint32(verAndHeroes, 3, bs, ii, cardLen, numHeroes)) {
				Etc.BEM("fail");
			}
			i = ii[0];
		}
//		System.out.println("heroes:" + numHeroes[0] + ",strlen=" + strLen);
		List heroes = new ArrayList();
		int[] prevCardBase = { 0 };
		for (int currHero = 0; currHero < numHeroes[0]; currHero++) {
			int[] heroTurn = { 0 };
			int[] heroCardID = { 0 };
			int[] ii = { i };
			if (!ReadSerializedCard(bs, ii, cardLen, prevCardBase, heroTurn, heroCardID)) {
				Etc.BEM("fail");// return false;
			}
			i = ii[0];
			heroes.add(new Object[] { heroCardID[0], heroTurn[0] });
			System.out.printf("hero=%s, turn=%d\n", getCardName(heroCardID[0]), heroTurn[0]);
		}
		List cards = new ArrayList();
		prevCardBase[0] = 0;
		int ccnt = 0;
		while (i < cardLen) {
			int[] cardCnt = { 0 };
			int[] cardId = { 0 };
			int[] ii = { i };
			if (!ReadSerializedCard(bs, ii, cnt, prevCardBase, cardCnt, cardId)) {
				Etc.BEM("fail");
			}
			i = ii[0];
			cards.add(new Object[] { cardCnt[0], cardId[0] });
			System.out.printf("card=%s, count=%d\n", getCardName(cardId[0]), cardCnt[0]);
			ccnt += cardCnt[0];
		}
		System.out.println("cards(not included cards on heroes):" + ccnt);
		String name = "";
		if (i <= cnt) {
			name = new String(bs, bs.length - strLen, strLen, "UTF-8");
		}
		System.out.println("name:" + name);
		return new Object[] { name, heroes, cards };
	}

	Map m;

	private Object getCardName(int i) throws Exception {
		if (m == null) {
			m = new CardSets().getCardMap();
		}
		return Config.get(m.get(i), "card_name.english");
	}

	private boolean ReadSerializedCard(byte[] bs, int[] start, int end, int[] prev, int[] outCnt, int[] outCardID) {
		if (start[0] > end) {
			return false;
		}
		int header = bs[start[0]++] & 0xff;
		boolean hasExtCnt = (header >> 6) == 3;
		int[] delta = { 0 };
		if (!ReadVarEncodedUint32((byte) header, 5, bs, start, end, delta)) {
			return false;
		}
		outCardID[0] = prev[0] + delta[0];
		if (hasExtCnt) {
			if (!ReadVarEncodedUint32((byte) 0, 0, bs, start, end, outCnt)) {
				return false;
			}
		} else {
			outCnt[0] = (header >> 6) + 1;
		}
		prev[0] = outCardID[0];
		return true;
	}

	private boolean ReadVarEncodedUint32(byte baseValue, int baseBits, byte[] bs, int[] start, int end, int[] out) {
		out[0] = 0;
		int delta = 0;
		if ((baseBits == 0) || ReadBitsChunk(baseValue, baseBits, delta, out)) {
			delta += baseBits;
			while (true) {
				if (start[0] > end) {
					return false;
				}
				byte next = bs[start[0]++];
				if (!ReadBitsChunk(next, 7, delta, out))
					break;
				delta += 7;
			}
		}
		return true;
	}

	private boolean ReadBitsChunk(int chunk, int numBits, int currShift, int[] outBit) {
		int contBit = 1 << numBits;
		int newBit = chunk & (contBit - 1);
		outBit[0] |= (newBit << currShift);
		return (chunk & contBit) != 0;
	}

}
