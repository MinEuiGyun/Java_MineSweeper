/**
 * Copyright (c) 2024 Name : Min Eui Gyun, Organization : CHUNGNAM NATIONAL UNIV.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import javax.swing.UIManager;
import java.awt.Color;

public class Main {
    // 메소드 이름: main
    // 메소드 기능1: UIManager를 사용하여 버튼의 배경색을 설정
    // 메소드 기능2: MinesweeperGame 객체를 생성하고 게임을 시작
    public static void main(String[] args) {
        UIManager.put("Button.background", Color.LIGHT_GRAY);
        UIManager.put("Button.opaque", true);

        MinesweeperGame game = new MinesweeperGame();
        game.start();
    }
}
