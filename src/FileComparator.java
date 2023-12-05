import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class FileComparator {
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);


        String file1Path = sc.next();
        String file2Path = sc.next();

        try {
            byte[] file1Content = Files.readAllBytes(Paths.get(file1Path));
            byte[] file2Content = Files.readAllBytes(Paths.get(file2Path));

            if (Arrays.equals(file1Content, file2Content)) {
                System.out.println("文件内容相同");
            } else {
                System.out.println("文件内容不同");
            }
        } catch (IOException e) {
            System.out.println("发生IO异常：" + e.getMessage());
        }
    }
}