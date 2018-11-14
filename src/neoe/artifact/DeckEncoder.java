package neoe.artifact;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import neoe.util.Base64;

/** port form C# port, seems work */
public class DeckEncoder {

	public static void main(String[] args) throws Exception {
		System.out.println(EncodedPrefix + EncodeDeck("test1", new int[][] { { 10213, 3, }, { 10212, 1 } },
				new int[][] { { 10540, 3, }, { 10210, 2 }, { 10211, 1 } }));

	}

	public static int CurrentVersion = 2;
	private static String EncodedPrefix = "ADC";
	private static int HeaderSize = 3;

	public static String EncodeDeck(String name, int[][] heroes, int[][] cards) throws Exception {
		byte[] bs = EncodeBytes(name, heroes, cards);
		return EncodeBytesToString(bs);
	}

	private static byte[] EncodeBytes(String name, int[][] heroes, int[][] cards) throws Exception {
		Arrays.sort(heroes, (p1, p2) -> (p1[0] - p2[0]));
		Arrays.sort(cards, (p1, p2) -> (p1[0] - p2[0]));

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		int version = CurrentVersion << 4 | ExtractNBitsWithCarry(heroes.length, 3);

		baos.write(version);

		int dummyChecksum = 0;
		int checksumByte = baos.size();
		baos.write(dummyChecksum);
		// write the name size
		int nameLen = name.length();
		if (nameLen > 63) {
			name = name.substring(0, 63).trim();
			nameLen = name.length();
		}
		baos.write(nameLen);

		AddRemainingNumberToBuffer(heroes.length, 3, baos);

		int prevCardId = 0;
		for (int[] row : heroes) {
			AddCardToBuffer(row[1], row[0] - prevCardId, baos);
			prevCardId = row[0];
		}

		prevCardId = 0;
		for (int[] row : cards) {
			AddCardToBuffer(row[1], row[0] - prevCardId, baos);
			prevCardId = row[0];
		}

		int preStringByteCount = baos.size();

		baos.write(name.getBytes("utf8"));
		byte[] bs = baos.toByteArray();
		bs[checksumByte] = (byte) ComputeChecksum(bs, preStringByteCount - HeaderSize);
		return bs;
	}

	private static String EncodeBytesToString(byte[] bs) {
		return Base64.encodeBytes(bs).replace('/', '-').replace('=', '_');
	}

	private static int ExtractNBitsWithCarry(int value, int numBits) {
		int limitBit = 1 << numBits;
		int result = (value & (limitBit - 1));
		if (value >= limitBit) {
			result |= limitBit;
		}
		return result;
	}

	private static void AddRemainingNumberToBuffer(int value, int alreadyWrittenBits, ByteArrayOutputStream baos) {
		value >>= alreadyWrittenBits;
		while (value > 0) {
			int nextByte = ExtractNBitsWithCarry(value, 7);
			value >>= 7;
			baos.write(nextByte);
		}
	}

	private static void AddCardToBuffer(int count, int value, ByteArrayOutputStream bytes) {

		int countBytesStart = bytes.size();

		int firstByteMaxCount = 0x03;
		boolean extendedCount = (count - 1) >= firstByteMaxCount;

		int firstByteCount = extendedCount ? firstByteMaxCount : /* ( uint8 ) */(count - 1);
		int firstByte = (firstByteCount << 6);
		firstByte |= ExtractNBitsWithCarry(value, 5);

		bytes.write(firstByte);

		AddRemainingNumberToBuffer(value, 5, bytes);

		if (extendedCount) {
			AddRemainingNumberToBuffer((int) count, 0, bytes);
		}

		int countBytesEnd = bytes.size();

		if (countBytesEnd - countBytesStart > 11) {
			throw new RuntimeException("fail");
		}
	}

	private static int ComputeChecksum(byte[] bytes, int numBytes) {
		int checksum = 0;
		for (int addCheck = HeaderSize; addCheck < numBytes + HeaderSize; addCheck++) {
			byte b = bytes[addCheck];
			checksum += b;
		}

		return checksum;
	}
}
