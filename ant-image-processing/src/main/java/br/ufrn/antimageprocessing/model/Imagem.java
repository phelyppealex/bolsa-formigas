package br.ufrn.antimageprocessing.model;

import lombok.Data;

@Data
public class Imagem {
    int[][][] imagem;
    double metrica;
    String descritores;
}
