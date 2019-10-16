package com.glroland.mlgame.ui;

public class Game {

	private int width;
	private int height;
	private int playerX;
	private int difficulty;
	private byte[][] board;

	public Game(int w, int h, int d) {
		width = w;
		height = h;

		playerX = w / 2;

		difficulty = d;

		board = new byte[height][width];
		byte[] line = randomLine();
		for (int x = 0; x < width; x++)
			board[0][x] = line[x];
	}

	private byte[] randomLine() {
		byte[] line = new byte[width];
		for (int i = 0; i < difficulty; i++) {
			int obsticle = (int) (Math.random() * width);
			line[obsticle] = 1;
		}

		return line;
	}

	public void moveLeft() {
		if (playerX > 0) {
			int adjacent = board[height - 1][playerX - 1];
			if (adjacent == 0)
				playerX--;
		}
	}

	public void moveRight() {
		if (playerX < (width - 1)) {
			int adjacent = board[height - 1][playerX + 1];
			if (adjacent == 0)
				playerX++;
		}
	}

	public void progressTime() throws CollisionException {
//		logGameBoard("Before");
		
		for (int y = (height - 1); y > 0 ; y--) {
			System.arraycopy(board[y-1], 0, board[y], 0, width);
		}

		byte[] newLine = randomLine();
		System.arraycopy(newLine,  0, board[0], 0, width);

//		System.out.println("Block - " + board[height - 1][playerX]);
		if (board[height - 1][playerX] != 0) {
			System.out.println("Collision");
			throw new CollisionException();
		}
		
//		logGameBoard("After");
	}

	public void logGameBoard(String msg) 
	{
		StringBuffer buffer = new StringBuffer();
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				buffer.append(Byte.toString(board[y][x]));
			}
			buffer.append("\n");
		}
		System.out.println("Game Board Contents <" + msg + ">\n" + buffer.toString());
	}
	
	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getPlayerX() {
		return playerX;
	}

	public void setPlayerX(int playerX) {
		this.playerX = playerX;
	}

	public byte[][] getBoard() {
		return board;
	}

	public void setBoard(byte[][] board) {
		this.board = board;
	}

}
