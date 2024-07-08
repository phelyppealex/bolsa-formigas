package br.ufrn.antimageprocessing.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.math3.stat.Frequency;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import lombok.Data;

@Data
public class Imagem {
    int[][][] imagem;
    int[][][] imagemContrastada;
    int[][][] imagemFinal;
    int[][][] mascara;
    int[] indicesAltura;
    int[] indicesLargura;
    int[] indicesMetrica;
    double metrica;
    String descritores;

    public Mat convertToOpenCV(int[][][] image){
        var imagemLong = new long[image.length][image[0].length][3];

        for(int i = 0; i < image.length; i++){
            for(int j = 0; j < image[0].length; j++){
                for(int x = 0; x < 3; x++){
                    imagemLong[i][j][x] = image[i][j][x];
                }
            }
        }

        return convertToOpenCV(imagemLong);
    }

    public Mat convertToOpenCV(long[][][] image) {
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

    public Mat convertToOpenCV(long[][] image) {
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

    public void convertToInt(Mat mat){
        for(int i = 0; i < imagem.length; i++){
            for(int j = 0; j < imagem[0].length; j++){
                for(int x = 0; x < 3; x++){
                    this.imagemContrastada[i][j][x] = mat.put(i,j,x);
                    //this.imagem[i][j][x] = mat.put(i,j,x);
                }
            }
        }
    }

    public int getModaHistograma(int[] hist){
        Frequency freq = new Frequency();

        for(int i: hist){
            freq.addValue(i);
        }

        List<Comparable<?>> modas = freq.getMode();

        if (!modas.isEmpty()) {
            Comparable<?> modaComparable = modas.get(0);
            if (modaComparable instanceof Long) {
                return ((Long) modaComparable).intValue();
            } else if (modaComparable instanceof Double) {
                return ((Double) modaComparable).intValue();
            } else {
                throw new IllegalStateException("Tipo inesperado encontrado como moda: " + modaComparable.getClass());
            }
        } else {
            throw new IllegalStateException("Nenhuma moda encontrada.");
        }
    }

    public int[][] mapearRegiao(boolean[][] mascara, int linha, int coluna, int rotulo, int[][] matrizRotulos){
        Queue<int[]> fila = new LinkedList<>();

        int[] primeiroFila = {linha, coluna};
        
        fila.offer(primeiroFila);
        matrizRotulos[fila.peek()[0]][fila.peek()[1]] = rotulo;

        while (!fila.isEmpty()){

            int i = fila.peek()[0],
                j = fila.peek()[1];

            // Cima
            if(linha > 0){
                if(mascara[i -1][j] == true && matrizRotulos[i -1][j] == 0){
                    int[] array = {i -1, j};
                    matrizRotulos[array[0]][array[1]] = rotulo;
                    fila.offer(array);
                }
            }

            // Baixo
            if (linha < mascara.length -1) {
                if(mascara[i +1][j] == true && matrizRotulos[i +1][j] == 0){
                    int[] array = {i +1, j};
                    matrizRotulos[array[0]][array[1]] = rotulo;
                    fila.offer(array);
                }                
            }
            
            // Esquerda
            if(coluna > 0){
                if(mascara[i][j -1] == true && matrizRotulos[i][j -1] == 0){
                    int[] array = {i, j -1};
                    matrizRotulos[array[0]][array[1]] = rotulo;
                    fila.offer(array);
                }
            }
            
            // Direita
            if (coluna < mascara[0].length -1){
                if(mascara[i][j +1] == true && matrizRotulos[i][j +1] == 0){
                    int[] array = {i, j +1};
                    matrizRotulos[array[0]][array[1]] = rotulo;
                    fila.offer(array);
                }
            }

            fila.poll();
        }
        
        int[][] nome = {{1, 1}, {1, 1}};
        return nome;
    }
}
