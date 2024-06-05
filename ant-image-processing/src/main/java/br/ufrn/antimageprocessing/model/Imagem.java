package br.ufrn.antimageprocessing.model;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import lombok.Data;

@Data
public class Imagem {
    int[][][] imagem;
    int[][][] imagemFinal;
    int[][][] mascara;
    int[] indicesAltura;
    double metrica;
    String descritores;

    public static Mat convertToOpenCV(int[][][] image) {
        // Cria uma matriz Mat com as dimensões corretas e o tipo de dados correto
        Mat mat = new Mat(image.length, image[0].length, CvType.CV_8UC(image[0][0].length));

        // Converte a matriz int[][][] para a matriz Mat
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                // Obtém os valores de intensidade de cada canal e os converte para byte (8 bits)
                byte[] pixelData = new byte[image[0][0].length];

                for (int k = 0; k < image[0][0].length; k++) {
                    pixelData[k] = (byte) image[i][j][k];
                }

                // Define os valores de pixel na matriz Mat
                mat.put(i, j, pixelData);
            }
        }

        return mat;
    }

    public static Mat convertToOpenCV(int[][] image) {
        // Cria uma matriz Mat com as dimensões corretas e o tipo de dados correto
        Mat mat = new Mat(image.length, image[0].length, CvType.CV_8UC(1));

        // Converte a matriz int[][][] para a matriz Mat
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                // Obtém os valores de intensidade de cada canal e os converte para byte (8 bits)

                // Define os valores de pixel na matriz Mat
                mat.put(i, j, image[i][j]);
            }
        }

        return mat;
    }
}
