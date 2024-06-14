package br.ufrn.antimageprocessing.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import br.ufrn.antimageprocessing.model.Imagem;
import br.ufrn.antimageprocessing.processing.Image;

@RestController
@RequestMapping("/imagem-capsula/")
public class ImagemCapsulaController {

    Imagem im;

    @GetMapping
    public Imagem getDescritores() {
        return im;
    }

    @PostMapping
    public Imagem processarImagem(@RequestBody Imagem imagem){
        im = imagem;

        // Total de colunas da pilha e cabeça na imagem
        int totalColunasPilha, totalColunasCabeca;
        // A variável métrica diz respeito ao tamanho da pilha em mm
        float
            metrica = (float) imagem.getMetrica(); //milímetros
        
        // Lendo do banco de imagens
        var imRGB = imagem.getImagem();

        // Convertendo a imagem pra Mat para utilizar a lib OpenCV
        // Mat imCsv = imagem.convertToOpenCV(imRGB);

        // double alpha = 1.5; // Fator de contraste
        // double beta = 0; // Viés do brilho
        // imCsv.convertTo(imCsv, -1, alpha, beta);

        // imagem.convertToInt(imCsv);

        //Convertendo a imagem para tons de cinza
        var imGray = Image.rgb2gray(imRGB);
        
        /*
        * Aqui acontecem 3 processos.
        * - Primeiro a imagem "imLimiarizada" é transformada em lógica
        * - É feito uma dilatação
        * - É feita uma erosão
        */

        var imLogica = Image.logical(imGray);

        for(int i = 0; i < imGray.length; i++){
            for(int j = 0; j < imGray[0].length; j++){
                imLogica[i][j] = !imLogica[i][j];
            }
        }

        imLogica = Image.bwClose(
            imLogica,
            9
        );

        imLogica = Image.bwOpen(
            imLogica,
            30
        );

        var imLogicaErros = Image.logical(imGray);

        for(int i = 0; i < imGray.length; i++)
            for(int j = 0; j < imGray[0].length; j++)
                if(imLogicaErros[i][j])
                    imLogica[i][j] = false;
        
        imLogica = Image.bwClose(
            imLogica,
            9
        );

        // Gerando imagem final com tons de cinza
        for(int i = 0; i < imGray.length; i++)
            for(int j = 0; j < imGray[0].length; j++)
                if(!imLogica[i][j])
                    imGray[i][j] = 0;
        
        // Gerando imagem final colorida
        for(int i = 0; i < imGray.length; i++)
            for(int j = 0; j < imGray[0].length; j++)
                if(!imLogica[i][j])
                    for(int z = 0; z < 3; z++)
                        imRGB[i][j][z] = 0;

        /*
        * Criando um vetor booleano do tamanho da largura
        * da imagem para que vejamos quais colunas possuem
        * pixels de foreground.
        */
        var fgPixels = new boolean[imLogica[0].length];
        for(int i = 0; i < imLogica.length; i++)
            for(int j = 0; j < imLogica[0].length; j++)
                if(imLogica[i][j] && !fgPixels[j])
                    fgPixels[j] = true;
        
        // Guardando índices de início e de fim das colunas correspondentes a pilha
        int inicioPilha = -1, fimPilha = -1;
        for(int i = fgPixels.length-1; i > 0; i--){
            if(fgPixels[i]){
                if(inicioPilha == -1)
                    inicioPilha = i;
                if(fgPixels[i-1] == false){
                    fimPilha = i;
                    break;
                }
            }
        }
        
        totalColunasPilha = inicioPilha-fimPilha+1;
        
        // Guardando índices de início e de fim das colunas correspondentes a cabeça
        int inicioCabeca = -1, fimCabeca = -1;
        for(int i = 0; i < fgPixels.length - 1; i++){
            if(fgPixels[i]){
                if(inicioCabeca == -1)
                    inicioCabeca = i;
                if(fgPixels[i+1] == false){
                    fimCabeca = i;
                    break;
                }
            }
        }

        totalColunasCabeca = fimCabeca-inicioCabeca+1;

        // Criando imagem com apenas a cabeça a partir dos indices obtidos anteriormente
        var cabeca = new boolean[imLogica.length][totalColunasCabeca];
        for(int i = 0; i < cabeca.length; i++)
            for(int j = 0; j < cabeca[0].length; j++)
                cabeca[i][j] = imLogica[i][j+inicioCabeca];

        /*
        *  Identificando vértice da cabeça
        */
        int regioesFrente = 0; // Verifica quantas regiões existem na linha em questão
        int[] indiceVertice = new int[2]; // Salva o índice do vértice
        boolean duasRegioes = false; // Verifica a primeira ocorência de duas regiões numa linha


        for(int i = 0; i < cabeca.length; i++){
            regioesFrente = 0;
            for(int j = inicioCabeca; j < fimCabeca; j++){
                if(imLogica[i][j] && !imLogica[i][j-1]){
                    regioesFrente++;
                    if(regioesFrente == 2){
                        duasRegioes = true;

                        indiceVertice[0] = i;
                        indiceVertice[1] = j;
                    }
                }
            }
            if(duasRegioes && regioesFrente == 1){
                break;
            }
        }

        /*
        * Fim da cabeça
        */
        int[] ultimoPixel = {0,0};
        boolean encontrou = false;
        for(int i = cabeca.length-1; i >= 0; i--){
            for(int j = inicioCabeca; j < fimCabeca; j++){
                if(imLogica[i][j]){
                    ultimoPixel[0] = i;
                    ultimoPixel[1] = j;
                    encontrou = true;
                }
            }
            if (encontrou)
                break;
        }

        /*
        * Resultados do processamento
        */

        /*
        * Aplicando a fórmula de pitágoras para
        * descobrir a distância do vértice ao fim
        * da mandíbula em pixels
        */
        double
            cateto1 = Math.abs(indiceVertice[0] - ultimoPixel[0]),
            cateto2 = Math.abs(indiceVertice[1] - ultimoPixel[1]);
        
        double alturaVerticeMandibula = Math.sqrt(
            Math.pow(cateto1,2) + Math.pow(cateto2,2)
        );

        double alturaVerticeMandibulaMm = alturaVerticeMandibula * metrica / totalColunasPilha;
        double larguraCabecaMm = totalColunasCabeca * metrica / totalColunasPilha;

        int[] indicesAltura = {indiceVertice[0], indiceVertice[1], ultimoPixel[0], ultimoPixel[1]};
        int[] indicesLargura = {
            indiceVertice[0],
            inicioCabeca,
            indiceVertice[0],
            fimCabeca
        };

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        DecimalFormat df = new DecimalFormat("#.##", symbols);

        String alturaVerticeMandibulaStr = df.format(alturaVerticeMandibulaMm);
        String larguraCabecaStr = df.format(larguraCabecaMm);

        im.setMascara(Image.bw2rgb(imLogica));

        im.setIndicesAltura(indicesAltura);
        im.setIndicesLargura(indicesLargura);

        im.setDescritores("Largura da cabeça: "+larguraCabecaStr+"mm");
        im.setDescritores(im.getDescritores() + "|Vértice ao fim da mandíbula: "+alturaVerticeMandibulaStr+"mm");
        
        im.setImagemFinal(imRGB);

        return im;
    }
}