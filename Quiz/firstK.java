import java.util.*;
public class firstK{
    public static String getFirstWords(String s, int k)
    {
        String words[]=s.split(' ');
        if(k>words.length)
        {
            k=words.length;
        }
        StringBuilder res=new StringBuilder();
        for(int i=0;i<k;i++){
            res.append(words[i]);
            if(i<k-1){
                res.append(" ");
            }
        }
        return res.toString();
    }
    public static void main(String args[]){
        String text="A quick brown fox jumps over the lazy dog";
        int k=4;
        System.out.println(getFirstWords(text,k));
    }
}