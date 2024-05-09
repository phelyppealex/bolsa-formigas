package br.ufrn.antimageprocessing.model;

import lombok.Data;

@Data
public class Imagem {
    int[][][] imagem;
    int[][][] imagemFinal;
    int[][][] mascara;
    double metrica;
    String descritores;
}
