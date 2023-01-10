package PiratePontoon;

import java.io.Serializable;

public class Card implements Serializable{
    String suit,rank;
    private int value; 
 
    //sets the rank and suit of a card
    public Card(String r,String s){
        suit=s;
        rank=r;
    }
    
    //sets the value of a card
    public void setValue(int val){
        value=val;
    }
 
    //returns the value of a card
    public int getValue(){
        return value;
    } 
 
    //returns a card as string
    public String ToString(){
        return rank+" of "+suit;
    }
}