package br.ufrn.antimageprocessing.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ufrn.antimageprocessing.model.Imagem;
import br.ufrn.antimageprocessing.processing.Image;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/imagem-mandibula/")
public class ImagemMandibulaController {

    Imagem im;

    @GetMapping
    public Imagem getDescritores() {
        return im;
    }
    
    @PostMapping
    public void processarImagem(@RequestBody Imagem imagem){
        im = imagem;

        int totalColunasPilha, totalColunasMandibula, totalLinhasMandibula;
        boolean achou = false;

        var imRGB = imagem.getImagem();

        var imGray = Image.rgb2gray(imRGB);

        var imLogica = Image.logical(imGray);

        for(int i = 0; i < imGray.length; i++){
            for(int j = 0; j < imGray[0].length; j++){
                imLogica[i][j] = !imLogica[i][j];
            }
        }

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
        var fgPixelsX = new boolean[imLogica[0].length];
        for(int i = 0; i < imLogica.length; i++)
            for(int j = 0; j < imLogica[0].length; j++)
                if(imLogica[i][j] && !fgPixelsX[j])
                    fgPixelsX[j] = true;
        
        // Guardando índices de início e de fim das colunas correspondentes a pilha
        int inicioPilha = -1, fimPilha = -1;
        for(int i = fgPixelsX.length-1; i > 0; i--){
            if(fgPixelsX[i]){
                if(inicioPilha == -1)
                    inicioPilha = i;
                if(fgPixelsX[i-1] == false){
                    fimPilha = i;
                    break;
                }
            }
        }
        
        totalColunasPilha = inicioPilha-fimPilha+1;
        
        // Guardando índices de início e de fim das colunas correspondentes a mandíbula
        int inicioMandibula = -1, fimMandibula = -1;
        for(int i = 0; i < fgPixelsX.length - 1; i++){
            if(fgPixelsX[i]){
                if(inicioMandibula == -1)
                    inicioMandibula = i;
                if(fgPixelsX[i+1] == false){
                    fimMandibula = i;
                    break;
                }
            }
        }

        totalColunasMandibula = fimMandibula-inicioMandibula+1;

        int iPixelMaisAlto = -1, iPixelMaisBaixo = -1;
        // Pegando o pixel de foreground mais alto da mandíbula
        for(int i = 0; i < imLogica.length; i++){
            for(int j = inicioMandibula; j <= fimMandibula; j++){
                if(imLogica[i][j]){
                    iPixelMaisAlto = i;
                    achou = true;
                }
            }
            if(achou){
                achou = false;
                break;
            }
        }

        // Pegando o pixel de foreground mais baixo da mandíbula
        for(int i = imLogica.length-1; i >= 0; i--){
            for(int j = inicioMandibula; j <= fimMandibula; j++){
                if(imLogica[i][j]){
                    iPixelMaisBaixo = i;
                    achou = true;
                }
            }
            if(achou){
                achou = false;
                break;
            }
        }

        var imagemFinal = new int[imRGB.length][imRGB[0].length][3];
        int[] zeros = {0,0,0};
        for(int i = 0; i < imRGB.length; i++){
            for(int j = 0; j < imRGB[0].length; j++){
                if(imLogica[i][j])
                    imagemFinal[i][j] = imRGB[i][j];
                else
                    imagemFinal[i][j] = zeros;
            }
        }

        totalLinhasMandibula = iPixelMaisBaixo - iPixelMaisAlto + 1;

        double alturaMandibulaMm = totalLinhasMandibula * imagem.getMetrica() / totalColunasPilha;
        double larguraMandibulaMm = totalColunasMandibula * imagem.getMetrica() / totalColunasPilha;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        DecimalFormat df = new DecimalFormat("#.##", symbols);

        String alturaMandibulaStr = df.format(alturaMandibulaMm);
        String larguraMandibulaStr = df.format(larguraMandibulaMm);

        im.setMascara(Image.bw2rgb(imLogica));
        im.setImagemFinal(imRGB);

        im.setDescritores("Altura da mandíbula: "+alturaMandibulaStr+"mm");
        im.setDescritores(im.getDescritores() + "|Largura da mandíbula: "+larguraMandibulaStr+"mm");
    }
}
