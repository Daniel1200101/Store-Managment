package main;

import java.util.Scanner;


public class IsValidFunctions {

	static Scanner s = new Scanner(System.in);

	public IsValidFunctions() {

	}

	public static int validInt() {
		boolean valid = false;
		int number = 0;
		while (!valid) {
			try {
				number = Integer.parseInt(s.nextLine());
				valid = true;
			} catch (Exception e) {
				System.out.printf("Integer input only\n");
			}
		}
		return number;
	}
	public static int isPositiveNumber() {
		boolean valid = false;
		int number = 0;
		while (!valid) {
				number =validInt();
				if(number>0)
					valid = true;	
		}
		return number;
	}
	public static boolean validBoolean(String bool) {
		if (bool.equals("true") || bool.equals("false"))
			return true;
		else
			return false;
	}
	public static Double validDouble() {
		boolean valid = false;
		double number = 0;
		while (!valid) {
			try {
				number = Double.parseDouble(s.nextLine());
				valid = true;
			} catch (Exception e) {
				System.out.printf("Double input only\n");
			}
		}
		return number;
	}

}
