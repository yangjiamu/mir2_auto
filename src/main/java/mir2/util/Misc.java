package mir2.util;

import mir2.map.MapTileInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** 一些不好归类的方法 */
public class Misc {
	/** log */

	/**
	 * 读取适合lwjgl窗口使用的图标
	 * 
	 * @param res
	 * 			资源路径
	 * @return bytebuffer
	 */
	public static ByteBuffer imageToIcon(String res) {
		try {
			InputStream is = new FileInputStream(new File(res));
			//InputStream is = Files.inputStream(res);
			BufferedImage bi = ImageIO.read(is);
			is.close();
			byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
			ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
			buffer.order(ByteOrder.nativeOrder());
			int pixelCount = data.length / 4;
			for(int i = 0; i < pixelCount; ++i) {
				// 将ARGB换成RGBA
				byte r = data[i * 4 + 1];
				byte g = data[i * 4 + 2];
				byte b = data[i * 4 + 3];
				byte a = data[i * 4];
				buffer.put(r).put(g).put(b).put(a);
			}
			buffer.flip();
			return buffer;
		} catch (IOException ex) {
		}
		return null;
	}
	
	/**
	 * 通过字节数组反序列化地图逻辑坐标块信息
	 * <br>
	 * 将资源索引-1，符合java数组集合索引下标从0开始的习惯
	 * 
	 * @param bytes
	 * 	数据(文件中直接读取，未经过任何处理的字节数组)
	 * @return
	 * 	地图逻辑坐标块儿信息
	 */
	public static MapTileInfo readMapTileInfo(byte[] bytes) throws IOException {
		MapTileInfo res = new MapTileInfo();
		// 读取背景
		short bng = readShort(bytes, 0, true);
		// 读取中间层
		short mid = readShort(bytes, 2, true);
		// 读取对象层
		short obj = readShort(bytes, 4, true);
		// 设置背景
		if((bng & 0x7fff) > 0) {
			res.setBngImgIdx((short) ((bng & 0x7fff) - 1));
			res.setHasBng(true);
		}
		// 设置中间层
		if((mid & 0x7fff) > 0) {
			res.setMidImgIdx((short) ((mid & 0x7fff) - 1));
			res.setHasMid(true);
		}
		// 设置对象层
		if((obj & 0x7fff) > 0) {
			res.setObjImgIdx((short) ((obj & 0x7fff) - 1));
			res.setHasObj(true);
		}
		// 设置是否可站立
		res.setCanWalk(!is1AtTopDigit(bng) && !is1AtTopDigit(obj));
		// 设置是否可飞行
		res.setCanFly(!is1AtTopDigit(obj));
		
		// 读取门索引(第7个byte)
		byte btTmp = bytes[6];
		if(is1AtTopDigit(btTmp)) {
			res.setDoorIdx((byte) (btTmp & 0x7F));
			res.setHasDoor(true);
		}
		// 读取门偏移(第8个byte)
		btTmp = bytes[7];
		res.setDoorOffset(btTmp);
		if(is1AtTopDigit(btTmp)) res.setDoorOpen(true);
		// 读取动画帧数(第9个byte)
		btTmp = bytes[8];
		res.setAniFrame(btTmp);
		if(is1AtTopDigit(btTmp)) {
			res.setAniFrame((byte) (btTmp & 0x7F));
			res.setHasAni(true);
		}
		// 读取并设置动画跳帧数(第10个byte)
		res.setAniTick(bytes[9]);
		// 读取资源文件索引(第11个byte)
		res.setObjFileIdx(bytes[10]);
		// 读取光照(第12个byte)
		res.setLight(bytes[11]);
		return res;
	}
	
	/**
	 * 生成纹理索引
	 * 
	 * @param textureType
	 * 			纹理类型，在本类中以TEXTURE_TYPE_开头的静态值
	 * @param textureFileIndex
	 * 			纹理文件索引
	 * @param textureIndex
	 * 			纹理索引
	 * @return	纹理索引
	 */
	public static Integer buildTextureIndex(int textureType, int textureFileIndex, short textureIndex) {
		return new Integer(textureType << 24 | textureFileIndex << 16 | textureIndex);
	}
	
	/**
	 * 判断一个整数是否为2的幂次
	 * 
	 * @param n
	 * 		需要验证的数
	 * @return 该数是否为2的幂次
	 */
	public static boolean isPowerOfTwo(int n) {
		return (n & (n-1)) == 0;
	}
	
	/**
	 * 获取最接近的2次幂整数
	 * 
	 * @param n
	 * 		目标数
	 * @return 与该数最接近的2幂次数(大于该数)
	 */
	public static int closestPowerOfTwo(int n) {
		int t = 2;
		while(t < n)
			t *= 2;
		return t;
	}
	
	/**
	 * 从字节数组中读取整形数据
	 * 
	 * @param bytes
	 * 	数据来源
	 * @param index
	 * 	数组中获取数据的起始位置(索引从0开始，数据还原将包含给定位置) 具体规则示例参见{@link #readShort(byte[], int, boolean) readShort}
	 * @param reverse
	 * 	是否需要反转字节(只针对包含当前数据的字节数组)
	 * @return
	 * 	从字节数组指定位置4个字节还原出的短整形数值
	 */
	public static int readInt(byte[] bytes, int index, boolean reverse) {
		if(reverse)
			return (int) (((bytes[index + 3] & 0xff) << 24)  
	                | ((bytes[index + 2] & 0xff) << 16)  
	                | ((bytes[index + 1] & 0xff) << 8) 
	                | (bytes[index] & 0xff));
		else
			return (int) (((bytes[index] & 0xff) << 24)  
	                | ((bytes[index + 1] & 0xff) << 16)  
	                | ((bytes[index + 2] & 0xff) << 8) 
	                | (bytes[index + 3] & 0xff));
	}
	
	/**
	 * 从字节数组中读取断整形数据
	 * 
	 * @param bytes
	 * 	数据来源
	 * @param index
	 * 	数组中获取数据的起始位置(索引从0开始，数据还原将包含给定位置)
	 * <br>
	 * 	例如给定[0x01,0x02,0x03,0x04],2,true作为参数则函数会将[0x04,0x03]还原为短整形数值并返回
	 * @param reverse
	 * 	是否需要反转字节(只针对包含当前数据的字节数组)
	 * @return
	 * 	从字节数组指定位置2个字节还原出的短整形数值
	 */
	public static short readShort(byte[] bytes, int index, boolean reverse) {
		if(reverse)
			return (short) ((bytes[index + 1] << 8) | (bytes[index] & 0xff));
		else
			return (short) ((bytes[index] << 8) | (bytes[index + 1] & 0xff));
	}

	/**
	 * 判断短整型数据最高位是否为1
	 * 
	 * @param target
	 * 	目标数据
	 * @return
	 * 	true表示该数据最高位为1，false表示该数据最高位为0
	 */
	public static boolean is1AtTopDigit(short target) {
		//return (target & 0b1000_0000_0000_0000) == 0b1000_0000_0000_0000;
		return (target & 0x8000) == 0x8000;
	}

	/**
	 * 判断字节数据最高位是否为1
	 * 
	 * @param target
	 * 	目标数据
	 * @return
	 * 	true表示该数据最高位为1，false表示该数据最高位为0
	 */
	public static boolean is1AtTopDigit(byte target) {
		//return (target & 0x1000_0000) == 0x1000_0000;
		return (target & 0x80) == 0x80;
	}
	
	/**
	 * 将颜色数换算成字节位
	 * 
	 * @param colorCount
	 * 	颜色数
	 * @return
	 * 	字节位
	 */
	public static int colorCountToBitCount(int colorCount) {
		if(colorCount == 256) return 8;
		else if(colorCount == 65536) return 16;
		else if(colorCount == 16777216) return 24;
		else return 32;
	}
	
	/**
	 * 对于bmp图片填充字节进行补足
	 * 
	 * @param bitCount
	 * 	每行图片色彩值字节位数(bit)
	 * @return
	 * 	每行图片色彩数据占字节数(byte)
	 */
	public static int widthBytes(int bitCount) {
		return (bitCount + 31) / 32 * 4;
	}
	
	/**
	 * 计算bmp图片逐行读取时需要跳过的字节数
	 * <br>
	 * 即用该行实际占用的字节数减去真正占用的字节数
	 * 
	 * @param bit
	 * 	位深度
	 * @param width
	 * 	图片宽度
	 * @return
	 * 	读取某行数据时需要跳过的字节数
	 * @see #widthBytes(int)
	 */
	public static int skipBytes(int bit, int width) {
		return widthBytes(bit * width) - width * (bit / 8);
	}

	/* Delphi 灰度图调色板，如果因设备不支持透明色可将调色板第一个颜色置为背景色，如-16777216表示黑色 */
	//public static int[] pallete = { 0, -8388608, -16744448, -8355840, -16777088, -8388480, -16744320, -4144960, -11173737, -6440504, -8686733, -13817559, -10857902, -10266022, -12437191, -14870504, -15200240, -14084072, -15726584, -886415, -2005153, -42406, -52943, -2729390, -7073792, -7067368, -13039616, -9236480, -4909056, -4365486, -12445680, -21863, -10874880, -9225943, -5944783, -7046285, -4369871, -11394800, -8703720, -13821936, -7583183, -7067392, -4378368, -3771566, -9752296, -3773630, -3257856, -5938375, -10866408, -14020608, -15398912, -12969984, -16252928, -14090240, -11927552, -6488064, -2359296, -2228224, -327680, -6524078, -7050422, -9221591, -11390696, -7583208, -7846895, -11919104, -14608368, -2714534, -3773663, -1086720, -35072, -5925756, -12439263, -15200248, -14084088, -14610432, -13031144, -7576775, -12441328, -9747944, -8697320, -7058944, -7568261, -9739430, -11910599, -14081768, -12175063, -4872812, -8688806, -3231340, -5927821, -7572646, -4877197, -2710157, -1071798, -1063284, -8690878, -9742791, -4352934, -10274560, -2701651, -11386327, -7052520, -1059155, -5927837, -10266038, -4348549, -10862056, -4355023, -13291223, -7043997, -8688822, -5927846, -10859991, -6522055, -12439280, -1069791, -15200256, -14081792, -6526208, -7044006, -11386344, -9741783, -8690911, -6522079, -2185984, -10857927, -13555440, -3228293, -10266055, -7044022, -3758807, -15688680, -12415926, -13530046, -15690711, -16246768, -16246760, -16242416, -15187415, -5917267, -9735309, -15193815, -15187382, -13548982, -10238242, -12263937, -7547153, -9213127, -532935, -528500, -530688, -9737382, -10842971, -12995089, -11887410, -13531979, -13544853, -2171178, -4342347, -7566204, -526370, -16775144, -16246727, -16248791, -16246784, -16242432, -16756059, -16745506, -15718070, -15713941, -15707508, -14591323, -15716006, -15711612, -13544828, -15195855, -11904389, -11375707, -14075549, -15709474, -14079711, -11908551, -14079720, -11908567, -8684734, -6513590, -10855895, -12434924, -13027072, -10921728, -3525332, -9735391, -14077696, -13551344, -13551336, -12432896, -11377896, -10849495, -13546984, -15195904, -15191808, -15189744, -10255286, -9716406, -10242742, -10240694, -10838966, -11891655, -10238390, -10234294, -11369398, -13536471, -10238374, -11354806, -15663360, -15193832, -11892662, -11868342, -16754176, -16742400, -16739328, -16720384, -16716288, -16712960, -11904364, -10259531, -8680234, -9733162, -8943361, -3750194, -7039844, -6515514, -13553351, -14083964, -15204220, -11910574, -11386245, -10265997, -3230217, -7570532, -8969524, -2249985, -1002454, -2162529, -1894477, -1040, -6250332, -8355712, -65536, -16711936, -256, -16776961, -65281, -16711681, -1 };
	/** 调色板，二维字节数组<br>每个颜色为ARGB格式 */
	public static byte[][] palletes = new byte[256][4];
	static {
		palletes[0][0] = 0;
		palletes[0][1] = 0;
		palletes[0][2] = 0;
		palletes[0][3] = 0;
		palletes[1][0] = -1;
		palletes[1][1] = -128;
		palletes[1][2] = 0;
		palletes[1][3] = 0;
		palletes[2][0] = -1;
		palletes[2][1] = 0;
		palletes[2][2] = -128;
		palletes[2][3] = 0;
		palletes[3][0] = -1;
		palletes[3][1] = -128;
		palletes[3][2] = -128;
		palletes[3][3] = 0;
		palletes[4][0] = -1;
		palletes[4][1] = 0;
		palletes[4][2] = 0;
		palletes[4][3] = -128;
		palletes[5][0] = -1;
		palletes[5][1] = -128;
		palletes[5][2] = 0;
		palletes[5][3] = -128;
		palletes[6][0] = -1;
		palletes[6][1] = 0;
		palletes[6][2] = -128;
		palletes[6][3] = -128;
		palletes[7][0] = -1;
		palletes[7][1] = -64;
		palletes[7][2] = -64;
		palletes[7][3] = -64;
		palletes[8][0] = -1;
		palletes[8][1] = 85;
		palletes[8][2] = -128;
		palletes[8][3] = -105;
		palletes[9][0] = -1;
		palletes[9][1] = -99;
		palletes[9][2] = -71;
		palletes[9][3] = -56;
		palletes[10][0] = -1;
		palletes[10][1] = 123;
		palletes[10][2] = 115;
		palletes[10][3] = 115;
		palletes[11][0] = -1;
		palletes[11][1] = 45;
		palletes[11][2] = 41;
		palletes[11][3] = 41;
		palletes[12][0] = -1;
		palletes[12][1] = 90;
		palletes[12][2] = 82;
		palletes[12][3] = 82;
		palletes[13][0] = -1;
		palletes[13][1] = 99;
		palletes[13][2] = 90;
		palletes[13][3] = 90;
		palletes[14][0] = -1;
		palletes[14][1] = 66;
		palletes[14][2] = 57;
		palletes[14][3] = 57;
		palletes[15][0] = -1;
		palletes[15][1] = 29;
		palletes[15][2] = 24;
		palletes[15][3] = 24;
		palletes[16][0] = -1;
		palletes[16][1] = 24;
		palletes[16][2] = 16;
		palletes[16][3] = 16;
		palletes[17][0] = -1;
		palletes[17][1] = 41;
		palletes[17][2] = 24;
		palletes[17][3] = 24;
		palletes[18][0] = -1;
		palletes[18][1] = 16;
		palletes[18][2] = 8;
		palletes[18][3] = 8;
		palletes[19][0] = -1;
		palletes[19][1] = -14;
		palletes[19][2] = 121;
		palletes[19][3] = 113;
		palletes[20][0] = -1;
		palletes[20][1] = -31;
		palletes[20][2] = 103;
		palletes[20][3] = 95;
		palletes[21][0] = -1;
		palletes[21][1] = -1;
		palletes[21][2] = 90;
		palletes[21][3] = 90;
		palletes[22][0] = -1;
		palletes[22][1] = -1;
		palletes[22][2] = 49;
		palletes[22][3] = 49;
		palletes[23][0] = -1;
		palletes[23][1] = -42;
		palletes[23][2] = 90;
		palletes[23][3] = 82;
		palletes[24][0] = -1;
		palletes[24][1] = -108;
		palletes[24][2] = 16;
		palletes[24][3] = 0;
		palletes[25][0] = -1;
		palletes[25][1] = -108;
		palletes[25][2] = 41;
		palletes[25][3] = 24;
		palletes[26][0] = -1;
		palletes[26][1] = 57;
		palletes[26][2] = 8;
		palletes[26][3] = 0;
		palletes[27][0] = -1;
		palletes[27][1] = 115;
		palletes[27][2] = 16;
		palletes[27][3] = 0;
		palletes[28][0] = -1;
		palletes[28][1] = -75;
		palletes[28][2] = 24;
		palletes[28][3] = 0;
		palletes[29][0] = -1;
		palletes[29][1] = -67;
		palletes[29][2] = 99;
		palletes[29][3] = 82;
		palletes[30][0] = -1;
		palletes[30][1] = 66;
		palletes[30][2] = 24;
		palletes[30][3] = 16;
		palletes[31][0] = -1;
		palletes[31][1] = -1;
		palletes[31][2] = -86;
		palletes[31][3] = -103;
		palletes[32][0] = -1;
		palletes[32][1] = 90;
		palletes[32][2] = 16;
		palletes[32][3] = 0;
		palletes[33][0] = -1;
		palletes[33][1] = 115;
		palletes[33][2] = 57;
		palletes[33][3] = 41;
		palletes[34][0] = -1;
		palletes[34][1] = -91;
		palletes[34][2] = 74;
		palletes[34][3] = 49;
		palletes[35][0] = -1;
		palletes[35][1] = -108;
		palletes[35][2] = 123;
		palletes[35][3] = 115;
		palletes[36][0] = -1;
		palletes[36][1] = -67;
		palletes[36][2] = 82;
		palletes[36][3] = 49;
		palletes[37][0] = -1;
		palletes[37][1] = 82;
		palletes[37][2] = 33;
		palletes[37][3] = 16;
		palletes[38][0] = -1;
		palletes[38][1] = 123;
		palletes[38][2] = 49;
		palletes[38][3] = 24;
		palletes[39][0] = -1;
		palletes[39][1] = 45;
		palletes[39][2] = 24;
		palletes[39][3] = 16;
		palletes[40][0] = -1;
		palletes[40][1] = -116;
		palletes[40][2] = 74;
		palletes[40][3] = 49;
		palletes[41][0] = -1;
		palletes[41][1] = -108;
		palletes[41][2] = 41;
		palletes[41][3] = 0;
		palletes[42][0] = -1;
		palletes[42][1] = -67;
		palletes[42][2] = 49;
		palletes[42][3] = 0;
		palletes[43][0] = -1;
		palletes[43][1] = -58;
		palletes[43][2] = 115;
		palletes[43][3] = 82;
		palletes[44][0] = -1;
		palletes[44][1] = 107;
		palletes[44][2] = 49;
		palletes[44][3] = 24;
		palletes[45][0] = -1;
		palletes[45][1] = -58;
		palletes[45][2] = 107;
		palletes[45][3] = 66;
		palletes[46][0] = -1;
		palletes[46][1] = -50;
		palletes[46][2] = 74;
		palletes[46][3] = 0;
		palletes[47][0] = -1;
		palletes[47][1] = -91;
		palletes[47][2] = 99;
		palletes[47][3] = 57;
		palletes[48][0] = -1;
		palletes[48][1] = 90;
		palletes[48][2] = 49;
		palletes[48][3] = 24;
		palletes[49][0] = -1;
		palletes[49][1] = 42;
		palletes[49][2] = 16;
		palletes[49][3] = 0;
		palletes[50][0] = -1;
		palletes[50][1] = 21;
		palletes[50][2] = 8;
		palletes[50][3] = 0;
		palletes[51][0] = -1;
		palletes[51][1] = 58;
		palletes[51][2] = 24;
		palletes[51][3] = 0;
		palletes[52][0] = -1;
		palletes[52][1] = 8;
		palletes[52][2] = 0;
		palletes[52][3] = 0;
		palletes[53][0] = -1;
		palletes[53][1] = 41;
		palletes[53][2] = 0;
		palletes[53][3] = 0;
		palletes[54][0] = -1;
		palletes[54][1] = 74;
		palletes[54][2] = 0;
		palletes[54][3] = 0;
		palletes[55][0] = -1;
		palletes[55][1] = -99;
		palletes[55][2] = 0;
		palletes[55][3] = 0;
		palletes[56][0] = -1;
		palletes[56][1] = -36;
		palletes[56][2] = 0;
		palletes[56][3] = 0;
		palletes[57][0] = -1;
		palletes[57][1] = -34;
		palletes[57][2] = 0;
		palletes[57][3] = 0;
		palletes[58][0] = -1;
		palletes[58][1] = -5;
		palletes[58][2] = 0;
		palletes[58][3] = 0;
		palletes[59][0] = -1;
		palletes[59][1] = -100;
		palletes[59][2] = 115;
		palletes[59][3] = 82;
		palletes[60][0] = -1;
		palletes[60][1] = -108;
		palletes[60][2] = 107;
		palletes[60][3] = 74;
		palletes[61][0] = -1;
		palletes[61][1] = 115;
		palletes[61][2] = 74;
		palletes[61][3] = 41;
		palletes[62][0] = -1;
		palletes[62][1] = 82;
		palletes[62][2] = 49;
		palletes[62][3] = 24;
		palletes[63][0] = -1;
		palletes[63][1] = -116;
		palletes[63][2] = 74;
		palletes[63][3] = 24;
		palletes[64][0] = -1;
		palletes[64][1] = -120;
		palletes[64][2] = 68;
		palletes[64][3] = 17;
		palletes[65][0] = -1;
		palletes[65][1] = 74;
		palletes[65][2] = 33;
		palletes[65][3] = 0;
		palletes[66][0] = -1;
		palletes[66][1] = 33;
		palletes[66][2] = 24;
		palletes[66][3] = 16;
		palletes[67][0] = -1;
		palletes[67][1] = -42;
		palletes[67][2] = -108;
		palletes[67][3] = 90;
		palletes[68][0] = -1;
		palletes[68][1] = -58;
		palletes[68][2] = 107;
		palletes[68][3] = 33;
		palletes[69][0] = -1;
		palletes[69][1] = -17;
		palletes[69][2] = 107;
		palletes[69][3] = 0;
		palletes[70][0] = -1;
		palletes[70][1] = -1;
		palletes[70][2] = 119;
		palletes[70][3] = 0;
		palletes[71][0] = -1;
		palletes[71][1] = -91;
		palletes[71][2] = -108;
		palletes[71][3] = -124;
		palletes[72][0] = -1;
		palletes[72][1] = 66;
		palletes[72][2] = 49;
		palletes[72][3] = 33;
		palletes[73][0] = -1;
		palletes[73][1] = 24;
		palletes[73][2] = 16;
		palletes[73][3] = 8;
		palletes[74][0] = -1;
		palletes[74][1] = 41;
		palletes[74][2] = 24;
		palletes[74][3] = 8;
		palletes[75][0] = -1;
		palletes[75][1] = 33;
		palletes[75][2] = 16;
		palletes[75][3] = 0;
		palletes[76][0] = -1;
		palletes[76][1] = 57;
		palletes[76][2] = 41;
		palletes[76][3] = 24;
		palletes[77][0] = -1;
		palletes[77][1] = -116;
		palletes[77][2] = 99;
		palletes[77][3] = 57;
		palletes[78][0] = -1;
		palletes[78][1] = 66;
		palletes[78][2] = 41;
		palletes[78][3] = 16;
		palletes[79][0] = -1;
		palletes[79][1] = 107;
		palletes[79][2] = 66;
		palletes[79][3] = 24;
		palletes[80][0] = -1;
		palletes[80][1] = 123;
		palletes[80][2] = 74;
		palletes[80][3] = 24;
		palletes[81][0] = -1;
		palletes[81][1] = -108;
		palletes[81][2] = 74;
		palletes[81][3] = 0;
		palletes[82][0] = -1;
		palletes[82][1] = -116;
		palletes[82][2] = -124;
		palletes[82][3] = 123;
		palletes[83][0] = -1;
		palletes[83][1] = 107;
		palletes[83][2] = 99;
		palletes[83][3] = 90;
		palletes[84][0] = -1;
		palletes[84][1] = 74;
		palletes[84][2] = 66;
		palletes[84][3] = 57;
		palletes[85][0] = -1;
		palletes[85][1] = 41;
		palletes[85][2] = 33;
		palletes[85][3] = 24;
		palletes[86][0] = -1;
		palletes[86][1] = 70;
		palletes[86][2] = 57;
		palletes[86][3] = 41;
		palletes[87][0] = -1;
		palletes[87][1] = -75;
		palletes[87][2] = -91;
		palletes[87][3] = -108;
		palletes[88][0] = -1;
		palletes[88][1] = 123;
		palletes[88][2] = 107;
		palletes[88][3] = 90;
		palletes[89][0] = -1;
		palletes[89][1] = -50;
		palletes[89][2] = -79;
		palletes[89][3] = -108;
		palletes[90][0] = -1;
		palletes[90][1] = -91;
		palletes[90][2] = -116;
		palletes[90][3] = 115;
		palletes[91][0] = -1;
		palletes[91][1] = -116;
		palletes[91][2] = 115;
		palletes[91][3] = 90;
		palletes[92][0] = -1;
		palletes[92][1] = -75;
		palletes[92][2] = -108;
		palletes[92][3] = 115;
		palletes[93][0] = -1;
		palletes[93][1] = -42;
		palletes[93][2] = -91;
		palletes[93][3] = 115;
		palletes[94][0] = -1;
		palletes[94][1] = -17;
		palletes[94][2] = -91;
		palletes[94][3] = 74;
		palletes[95][0] = -1;
		palletes[95][1] = -17;
		palletes[95][2] = -58;
		palletes[95][3] = -116;
		palletes[96][0] = -1;
		palletes[96][1] = 123;
		palletes[96][2] = 99;
		palletes[96][3] = 66;
		palletes[97][0] = -1;
		palletes[97][1] = 107;
		palletes[97][2] = 86;
		palletes[97][3] = 57;
		palletes[98][0] = -1;
		palletes[98][1] = -67;
		palletes[98][2] = -108;
		palletes[98][3] = 90;
		palletes[99][0] = -1;
		palletes[99][1] = 99;
		palletes[99][2] = 57;
		palletes[99][3] = 0;
		palletes[100][0] = -1;
		palletes[100][1] = -42;
		palletes[100][2] = -58;
		palletes[100][3] = -83;
		palletes[101][0] = -1;
		palletes[101][1] = 82;
		palletes[101][2] = 66;
		palletes[101][3] = 41;
		palletes[102][0] = -1;
		palletes[102][1] = -108;
		palletes[102][2] = 99;
		palletes[102][3] = 24;
		palletes[103][0] = -1;
		palletes[103][1] = -17;
		palletes[103][2] = -42;
		palletes[103][3] = -83;
		palletes[104][0] = -1;
		palletes[104][1] = -91;
		palletes[104][2] = -116;
		palletes[104][3] = 99;
		palletes[105][0] = -1;
		palletes[105][1] = 99;
		palletes[105][2] = 90;
		palletes[105][3] = 74;
		palletes[106][0] = -1;
		palletes[106][1] = -67;
		palletes[106][2] = -91;
		palletes[106][3] = 123;
		palletes[107][0] = -1;
		palletes[107][1] = 90;
		palletes[107][2] = 66;
		palletes[107][3] = 24;
		palletes[108][0] = -1;
		palletes[108][1] = -67;
		palletes[108][2] = -116;
		palletes[108][3] = 49;
		palletes[109][0] = -1;
		palletes[109][1] = 53;
		palletes[109][2] = 49;
		palletes[109][3] = 41;
		palletes[110][0] = -1;
		palletes[110][1] = -108;
		palletes[110][2] = -124;
		palletes[110][3] = 99;
		palletes[111][0] = -1;
		palletes[111][1] = 123;
		palletes[111][2] = 107;
		palletes[111][3] = 74;
		palletes[112][0] = -1;
		palletes[112][1] = -91;
		palletes[112][2] = -116;
		palletes[112][3] = 90;
		palletes[113][0] = -1;
		palletes[113][1] = 90;
		palletes[113][2] = 74;
		palletes[113][3] = 41;
		palletes[114][0] = -1;
		palletes[114][1] = -100;
		palletes[114][2] = 123;
		palletes[114][3] = 57;
		palletes[115][0] = -1;
		palletes[115][1] = 66;
		palletes[115][2] = 49;
		palletes[115][3] = 16;
		palletes[116][0] = -1;
		palletes[116][1] = -17;
		palletes[116][2] = -83;
		palletes[116][3] = 33;
		palletes[117][0] = -1;
		palletes[117][1] = 24;
		palletes[117][2] = 16;
		palletes[117][3] = 0;
		palletes[118][0] = -1;
		palletes[118][1] = 41;
		palletes[118][2] = 33;
		palletes[118][3] = 0;
		palletes[119][0] = -1;
		palletes[119][1] = -100;
		palletes[119][2] = 107;
		palletes[119][3] = 0;
		palletes[120][0] = -1;
		palletes[120][1] = -108;
		palletes[120][2] = -124;
		palletes[120][3] = 90;
		palletes[121][0] = -1;
		palletes[121][1] = 82;
		palletes[121][2] = 66;
		palletes[121][3] = 24;
		palletes[122][0] = -1;
		palletes[122][1] = 107;
		palletes[122][2] = 90;
		palletes[122][3] = 41;
		palletes[123][0] = -1;
		palletes[123][1] = 123;
		palletes[123][2] = 99;
		palletes[123][3] = 33;
		palletes[124][0] = -1;
		palletes[124][1] = -100;
		palletes[124][2] = 123;
		palletes[124][3] = 33;
		palletes[125][0] = -1;
		palletes[125][1] = -34;
		palletes[125][2] = -91;
		palletes[125][3] = 0;
		palletes[126][0] = -1;
		palletes[126][1] = 90;
		palletes[126][2] = 82;
		palletes[126][3] = 57;
		palletes[127][0] = -1;
		palletes[127][1] = 49;
		palletes[127][2] = 41;
		palletes[127][3] = 16;
		palletes[128][0] = -1;
		palletes[128][1] = -50;
		palletes[128][2] = -67;
		palletes[128][3] = 123;
		palletes[129][0] = -1;
		palletes[129][1] = 99;
		palletes[129][2] = 90;
		palletes[129][3] = 57;
		palletes[130][0] = -1;
		palletes[130][1] = -108;
		palletes[130][2] = -124;
		palletes[130][3] = 74;
		palletes[131][0] = -1;
		palletes[131][1] = -58;
		palletes[131][2] = -91;
		palletes[131][3] = 41;
		palletes[132][0] = -1;
		palletes[132][1] = 16;
		palletes[132][2] = -100;
		palletes[132][3] = 24;
		palletes[133][0] = -1;
		palletes[133][1] = 66;
		palletes[133][2] = -116;
		palletes[133][3] = 74;
		palletes[134][0] = -1;
		palletes[134][1] = 49;
		palletes[134][2] = -116;
		palletes[134][3] = 66;
		palletes[135][0] = -1;
		palletes[135][1] = 16;
		palletes[135][2] = -108;
		palletes[135][3] = 41;
		palletes[136][0] = -1;
		palletes[136][1] = 8;
		palletes[136][2] = 24;
		palletes[136][3] = 16;
		palletes[137][0] = -1;
		palletes[137][1] = 8;
		palletes[137][2] = 24;
		palletes[137][3] = 24;
		palletes[138][0] = -1;
		palletes[138][1] = 8;
		palletes[138][2] = 41;
		palletes[138][3] = 16;
		palletes[139][0] = -1;
		palletes[139][1] = 24;
		palletes[139][2] = 66;
		palletes[139][3] = 41;
		palletes[140][0] = -1;
		palletes[140][1] = -91;
		palletes[140][2] = -75;
		palletes[140][3] = -83;
		palletes[141][0] = -1;
		palletes[141][1] = 107;
		palletes[141][2] = 115;
		palletes[141][3] = 115;
		palletes[142][0] = -1;
		palletes[142][1] = 24;
		palletes[142][2] = 41;
		palletes[142][3] = 41;
		palletes[143][0] = -1;
		palletes[143][1] = 24;
		palletes[143][2] = 66;
		palletes[143][3] = 74;
		palletes[144][0] = -1;
		palletes[144][1] = 49;
		palletes[144][2] = 66;
		palletes[144][3] = 74;
		palletes[145][0] = -1;
		palletes[145][1] = 99;
		palletes[145][2] = -58;
		palletes[145][3] = -34;
		palletes[146][0] = -1;
		palletes[146][1] = 68;
		palletes[146][2] = -35;
		palletes[146][3] = -1;
		palletes[147][0] = -1;
		palletes[147][1] = -116;
		palletes[147][2] = -42;
		palletes[147][3] = -17;
		palletes[148][0] = -1;
		palletes[148][1] = 115;
		palletes[148][2] = 107;
		palletes[148][3] = 57;
		palletes[149][0] = -1;
		palletes[149][1] = -9;
		palletes[149][2] = -34;
		palletes[149][3] = 57;
		palletes[150][0] = -1;
		palletes[150][1] = -9;
		palletes[150][2] = -17;
		palletes[150][3] = -116;
		palletes[151][0] = -1;
		palletes[151][1] = -9;
		palletes[151][2] = -25;
		palletes[151][3] = 0;
		palletes[152][0] = -1;
		palletes[152][1] = 107;
		palletes[152][2] = 107;
		palletes[152][3] = 90;
		palletes[153][0] = -1;
		palletes[153][1] = 90;
		palletes[153][2] = -116;
		palletes[153][3] = -91;
		palletes[154][0] = -1;
		palletes[154][1] = 57;
		palletes[154][2] = -75;
		palletes[154][3] = -17;
		palletes[155][0] = -1;
		palletes[155][1] = 74;
		palletes[155][2] = -100;
		palletes[155][3] = -50;
		palletes[156][0] = -1;
		palletes[156][1] = 49;
		palletes[156][2] = -124;
		palletes[156][3] = -75;
		palletes[157][0] = -1;
		palletes[157][1] = 49;
		palletes[157][2] = 82;
		palletes[157][3] = 107;
		palletes[158][0] = -1;
		palletes[158][1] = -34;
		palletes[158][2] = -34;
		palletes[158][3] = -42;
		palletes[159][0] = -1;
		palletes[159][1] = -67;
		palletes[159][2] = -67;
		palletes[159][3] = -75;
		palletes[160][0] = -1;
		palletes[160][1] = -116;
		palletes[160][2] = -116;
		palletes[160][3] = -124;
		palletes[161][0] = -1;
		palletes[161][1] = -9;
		palletes[161][2] = -9;
		palletes[161][3] = -34;
		palletes[162][0] = -1;
		palletes[162][1] = 0;
		palletes[162][2] = 8;
		palletes[162][3] = 24;
		palletes[163][0] = -1;
		palletes[163][1] = 8;
		palletes[163][2] = 24;
		palletes[163][3] = 57;
		palletes[164][0] = -1;
		palletes[164][1] = 8;
		palletes[164][2] = 16;
		palletes[164][3] = 41;
		palletes[165][0] = -1;
		palletes[165][1] = 8;
		palletes[165][2] = 24;
		palletes[165][3] = 0;
		palletes[166][0] = -1;
		palletes[166][1] = 8;
		palletes[166][2] = 41;
		palletes[166][3] = 0;
		palletes[167][0] = -1;
		palletes[167][1] = 0;
		palletes[167][2] = 82;
		palletes[167][3] = -91;
		palletes[168][0] = -1;
		palletes[168][1] = 0;
		palletes[168][2] = 123;
		palletes[168][3] = -34;
		palletes[169][0] = -1;
		palletes[169][1] = 16;
		palletes[169][2] = 41;
		palletes[169][3] = 74;
		palletes[170][0] = -1;
		palletes[170][1] = 16;
		palletes[170][2] = 57;
		palletes[170][3] = 107;
		palletes[171][0] = -1;
		palletes[171][1] = 16;
		palletes[171][2] = 82;
		palletes[171][3] = -116;
		palletes[172][0] = -1;
		palletes[172][1] = 33;
		palletes[172][2] = 90;
		palletes[172][3] = -91;
		palletes[173][0] = -1;
		palletes[173][1] = 16;
		palletes[173][2] = 49;
		palletes[173][3] = 90;
		palletes[174][0] = -1;
		palletes[174][1] = 16;
		palletes[174][2] = 66;
		palletes[174][3] = -124;
		palletes[175][0] = -1;
		palletes[175][1] = 49;
		palletes[175][2] = 82;
		palletes[175][3] = -124;
		palletes[176][0] = -1;
		palletes[176][1] = 24;
		palletes[176][2] = 33;
		palletes[176][3] = 49;
		palletes[177][0] = -1;
		palletes[177][1] = 74;
		palletes[177][2] = 90;
		palletes[177][3] = 123;
		palletes[178][0] = -1;
		palletes[178][1] = 82;
		palletes[178][2] = 107;
		palletes[178][3] = -91;
		palletes[179][0] = -1;
		palletes[179][1] = 41;
		palletes[179][2] = 57;
		palletes[179][3] = 99;
		palletes[180][0] = -1;
		palletes[180][1] = 16;
		palletes[180][2] = 74;
		palletes[180][3] = -34;
		palletes[181][0] = -1;
		palletes[181][1] = 41;
		palletes[181][2] = 41;
		palletes[181][3] = 33;
		palletes[182][0] = -1;
		palletes[182][1] = 74;
		palletes[182][2] = 74;
		palletes[182][3] = 57;
		palletes[183][0] = -1;
		palletes[183][1] = 41;
		palletes[183][2] = 41;
		palletes[183][3] = 24;
		palletes[184][0] = -1;
		palletes[184][1] = 74;
		palletes[184][2] = 74;
		palletes[184][3] = 41;
		palletes[185][0] = -1;
		palletes[185][1] = 123;
		palletes[185][2] = 123;
		palletes[185][3] = 66;
		palletes[186][0] = -1;
		palletes[186][1] = -100;
		palletes[186][2] = -100;
		palletes[186][3] = 74;
		palletes[187][0] = -1;
		palletes[187][1] = 90;
		palletes[187][2] = 90;
		palletes[187][3] = 41;
		palletes[188][0] = -1;
		palletes[188][1] = 66;
		palletes[188][2] = 66;
		palletes[188][3] = 20;
		palletes[189][0] = -1;
		palletes[189][1] = 57;
		palletes[189][2] = 57;
		palletes[189][3] = 0;
		palletes[190][0] = -1;
		palletes[190][1] = 89;
		palletes[190][2] = 89;
		palletes[190][3] = 0;
		palletes[191][0] = -1;
		palletes[191][1] = -54;
		palletes[191][2] = 53;
		palletes[191][3] = 44;
		palletes[192][0] = -1;
		palletes[192][1] = 107;
		palletes[192][2] = 115;
		palletes[192][3] = 33;
		palletes[193][0] = -1;
		palletes[193][1] = 41;
		palletes[193][2] = 49;
		palletes[193][3] = 0;
		palletes[194][0] = -1;
		palletes[194][1] = 49;
		palletes[194][2] = 57;
		palletes[194][3] = 16;
		palletes[195][0] = -1;
		palletes[195][1] = 49;
		palletes[195][2] = 57;
		palletes[195][3] = 24;
		palletes[196][0] = -1;
		palletes[196][1] = 66;
		palletes[196][2] = 74;
		palletes[196][3] = 0;
		palletes[197][0] = -1;
		palletes[197][1] = 82;
		palletes[197][2] = 99;
		palletes[197][3] = 24;
		palletes[198][0] = -1;
		palletes[198][1] = 90;
		palletes[198][2] = 115;
		palletes[198][3] = 41;
		palletes[199][0] = -1;
		palletes[199][1] = 49;
		palletes[199][2] = 74;
		palletes[199][3] = 24;
		palletes[200][0] = -1;
		palletes[200][1] = 24;
		palletes[200][2] = 33;
		palletes[200][3] = 0;
		palletes[201][0] = -1;
		palletes[201][1] = 24;
		palletes[201][2] = 49;
		palletes[201][3] = 0;
		palletes[202][0] = -1;
		palletes[202][1] = 24;
		palletes[202][2] = 57;
		palletes[202][3] = 16;
		palletes[203][0] = -1;
		palletes[203][1] = 99;
		palletes[203][2] = -124;
		palletes[203][3] = 74;
		palletes[204][0] = -1;
		palletes[204][1] = 107;
		palletes[204][2] = -67;
		palletes[204][3] = 74;
		palletes[205][0] = -1;
		palletes[205][1] = 99;
		palletes[205][2] = -75;
		palletes[205][3] = 74;
		palletes[206][0] = -1;
		palletes[206][1] = 99;
		palletes[206][2] = -67;
		palletes[206][3] = 74;
		palletes[207][0] = -1;
		palletes[207][1] = 90;
		palletes[207][2] = -100;
		palletes[207][3] = 74;
		palletes[208][0] = -1;
		palletes[208][1] = 74;
		palletes[208][2] = -116;
		palletes[208][3] = 57;
		palletes[209][0] = -1;
		palletes[209][1] = 99;
		palletes[209][2] = -58;
		palletes[209][3] = 74;
		palletes[210][0] = -1;
		palletes[210][1] = 99;
		palletes[210][2] = -42;
		palletes[210][3] = 74;
		palletes[211][0] = -1;
		palletes[211][1] = 82;
		palletes[211][2] = -124;
		palletes[211][3] = 74;
		palletes[212][0] = -1;
		palletes[212][1] = 49;
		palletes[212][2] = 115;
		palletes[212][3] = 41;
		palletes[213][0] = -1;
		palletes[213][1] = 99;
		palletes[213][2] = -58;
		palletes[213][3] = 90;
		palletes[214][0] = -1;
		palletes[214][1] = 82;
		palletes[214][2] = -67;
		palletes[214][3] = 74;
		palletes[215][0] = -1;
		palletes[215][1] = 16;
		palletes[215][2] = -1;
		palletes[215][3] = 0;
		palletes[216][0] = -1;
		palletes[216][1] = 24;
		palletes[216][2] = 41;
		palletes[216][3] = 24;
		palletes[217][0] = -1;
		palletes[217][1] = 74;
		palletes[217][2] = -120;
		palletes[217][3] = 74;
		palletes[218][0] = -1;
		palletes[218][1] = 74;
		palletes[218][2] = -25;
		palletes[218][3] = 74;
		palletes[219][0] = -1;
		palletes[219][1] = 0;
		palletes[219][2] = 90;
		palletes[219][3] = 0;
		palletes[220][0] = -1;
		palletes[220][1] = 0;
		palletes[220][2] = -120;
		palletes[220][3] = 0;
		palletes[221][0] = -1;
		palletes[221][1] = 0;
		palletes[221][2] = -108;
		palletes[221][3] = 0;
		palletes[222][0] = -1;
		palletes[222][1] = 0;
		palletes[222][2] = -34;
		palletes[222][3] = 0;
		palletes[223][0] = -1;
		palletes[223][1] = 0;
		palletes[223][2] = -18;
		palletes[223][3] = 0;
		palletes[224][0] = -1;
		palletes[224][1] = 0;
		palletes[224][2] = -5;
		palletes[224][3] = 0;
		palletes[225][0] = -1;
		palletes[225][1] = 74;
		palletes[225][2] = 90;
		palletes[225][3] = -108;
		palletes[226][0] = -1;
		palletes[226][1] = 99;
		palletes[226][2] = 115;
		palletes[226][3] = -75;
		palletes[227][0] = -1;
		palletes[227][1] = 123;
		palletes[227][2] = -116;
		palletes[227][3] = -42;
		palletes[228][0] = -1;
		palletes[228][1] = 107;
		palletes[228][2] = 123;
		palletes[228][3] = -42;
		palletes[229][0] = -1;
		palletes[229][1] = 119;
		palletes[229][2] = -120;
		palletes[229][3] = -1;
		palletes[230][0] = -1;
		palletes[230][1] = -58;
		palletes[230][2] = -58;
		palletes[230][3] = -50;
		palletes[231][0] = -1;
		palletes[231][1] = -108;
		palletes[231][2] = -108;
		palletes[231][3] = -100;
		palletes[232][0] = -1;
		palletes[232][1] = -100;
		palletes[232][2] = -108;
		palletes[232][3] = -58;
		palletes[233][0] = -1;
		palletes[233][1] = 49;
		palletes[233][2] = 49;
		palletes[233][3] = 57;
		palletes[234][0] = -1;
		palletes[234][1] = 41;
		palletes[234][2] = 24;
		palletes[234][3] = -124;
		palletes[235][0] = -1;
		palletes[235][1] = 24;
		palletes[235][2] = 0;
		palletes[235][3] = -124;
		palletes[236][0] = -1;
		palletes[236][1] = 74;
		palletes[236][2] = 66;
		palletes[236][3] = 82;
		palletes[237][0] = -1;
		palletes[237][1] = 82;
		palletes[237][2] = 66;
		palletes[237][3] = 123;
		palletes[238][0] = -1;
		palletes[238][1] = 99;
		palletes[238][2] = 90;
		palletes[238][3] = 115;
		palletes[239][0] = -1;
		palletes[239][1] = -50;
		palletes[239][2] = -75;
		palletes[239][3] = -9;
		palletes[240][0] = -1;
		palletes[240][1] = -116;
		palletes[240][2] = 123;
		palletes[240][3] = -100;
		palletes[241][0] = -1;
		palletes[241][1] = 119;
		palletes[241][2] = 34;
		palletes[241][3] = -52;
		palletes[242][0] = -1;
		palletes[242][1] = -35;
		palletes[242][2] = -86;
		palletes[242][3] = -1;
		palletes[243][0] = -1;
		palletes[243][1] = -16;
		palletes[243][2] = -76;
		palletes[243][3] = 42;
		palletes[244][0] = -1;
		palletes[244][1] = -33;
		palletes[244][2] = 0;
		palletes[244][3] = -97;
		palletes[245][0] = -1;
		palletes[245][1] = -29;
		palletes[245][2] = 23;
		palletes[245][3] = -77;
		palletes[246][0] = -1;
		palletes[246][1] = -1;
		palletes[246][2] = -5;
		palletes[246][3] = -16;
		palletes[247][0] = -1;
		palletes[247][1] = -96;
		palletes[247][2] = -96;
		palletes[247][3] = -92;
		palletes[248][0] = -1;
		palletes[248][1] = -128;
		palletes[248][2] = -128;
		palletes[248][3] = -128;
		palletes[249][0] = -1;
		palletes[249][1] = -1;
		palletes[249][2] = 0;
		palletes[249][3] = 0;
		palletes[250][0] = -1;
		palletes[250][1] = 0;
		palletes[250][2] = -1;
		palletes[250][3] = 0;
		palletes[251][0] = -1;
		palletes[251][1] = -1;
		palletes[251][2] = -1;
		palletes[251][3] = 0;
		palletes[252][0] = -1;
		palletes[252][1] = 0;
		palletes[252][2] = 0;
		palletes[252][3] = -1;
		palletes[253][0] = -1;
		palletes[253][1] = -1;
		palletes[253][2] = 0;
		palletes[253][3] = -1;
		palletes[254][0] = -1;
		palletes[254][1] = 0;
		palletes[254][2] = -1;
		palletes[254][3] = -1;
		palletes[255][0] = -1;
		palletes[255][1] = -1;
		palletes[255][2] = -1;
		palletes[255][3] = -1;
	}
}
