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

        int totalColunasPilha, totalColunasCabeca, iMenorIntensidade = -1, iMaiorIntensidade = -1, limiar = 0, maiorVale;

        // A variável métrica diz respeito ao tamanho da pilha em mm (esta métrica é recebida do cliente)
        float
            metrica = (float) imagem.getMetrica(); //milímetros
        
        // Lendo imagem que veio do cliente
        var imRGB = imagem.getImagem();

        //Convertendo a imagem para tons de cinza
        var imGray = Image.rgb2gray(imRGB);

        // INICIANDO ABAIXO UM ALGORITMO DE LIMIARIZAÇÃO A PARTIR DO HISTOGRAMA

        // Criando histograma da imagem
        var imHist = Image.imHist(imGray);

        // Criando cópias do histograma anterior para conseguir encontrar os picos
        var novoHist1 = new int[256];
        var novoHist2 = new int[256];

        // Inicializando os histogramas
        for(int i = 0; i < 256; i++){
            novoHist1[i] = imHist[i];
            novoHist2[i] = imHist[i];
        }

        // Manipulando o histograma 1
        for(int i = 1; i < imHist.length; i++)
            if(novoHist1[i-1] > novoHist1[i])
                novoHist1[i] = novoHist1[i-1];
        
        // Manipulando o histograma 2
        for(int i = imHist.length-2; i >= 0; i--)
            if(novoHist2[i+1] > novoHist2[i])
                novoHist2[i] = novoHist2[i+1];

        /*
         * Pegando as modas de cada novo histograma que foi manipulado
         * para encontrar os dois maiores picos do histograma e fazer
         * a limiarização a partir do ponto mais baixo entre eles.
         */
        int moda1 = imagem.getModaHistograma(novoHist1);
        int moda2 = imagem.getModaHistograma(novoHist2);

        // Obtendo o indice do primeiro pico
        for(int i = 0; i < imHist.length; i++){
            if(imHist[i] == moda1){
                iMenorIntensidade = i;
                break;
            }
        }

        // Obtendo o indice do segundo pico
        for(int i = imHist.length-1; i >= 0; i--){
            if(imHist[i] == moda2){
                iMaiorIntensidade = i;
                break;
            }
        }

        /*
         * Algoritmo para solucionar o problema do caso de a moda 1
         * seja igual a moda 2 (o programa só conseguiu detectar um pico).
         * Neste caso é inicializado um novo histograma removendo a segunda
         * moda para conseguir encontrar a média 1 enfim.
         */
        if(moda1 == moda2){
            int[] novoArray = new int[iMaiorIntensidade+1];
            for(int i = 0; i < novoArray.length; i++){
                novoArray[i] = novoHist1[i];
            }

            moda1 = imagem.getModaHistograma(novoArray);

            // Corrigindo o indice do primeiro pico
            for(int i = 0; i < imHist.length; i++){
                if(imHist[i] == moda1){
                    iMenorIntensidade = i;
                    break;
                }
            }
        }

        System.out.println("Moda 1: ["+iMenorIntensidade+"] "+moda1);
        System.out.println("Moda 2: ["+iMaiorIntensidade+"] "+moda2);

        // Inicializando o maior vale com a intensidade do primeiro pico
        maiorVale = imHist[iMenorIntensidade];

        // Encontrando o menor vale e definindo o limiar da máscara principal
        for(int i = iMenorIntensidade + 1; i <= iMaiorIntensidade; i++){
            if(maiorVale > imHist[i]){
                maiorVale = imHist[i];
                limiar = i;
            }
        }

        // String stringHist = "";

        // for(int i = 0; i < imHist.length; i++){
        //     stringHist += imHist[i];
        //     if(i != imHist.length -1){
        //         stringHist += ";";
        //     }
        // }

        // Fazendo segmentação a partir do limiar encontrado
        for(int i = 0; i < imGray.length; i++)
            for(int j = 0; j < imGray[0].length; j++)
                if(imGray[i][j] <= limiar)
                    imGray[i][j] = 255;
                else
                    imGray[i][j] = 0;
        
        /*
         * Temos abaixo a criação de duas máscaras: a primeira é para
         * identificar melhor a formiga, porque em alguns momentos é
         * complicado identificar um limiar que consiga pegar a pilha 
         * também; a segunda é apenas para conseguir resgatar a pilha
         * que as vezes é perdida na primeira máscara. Em seguida, as duas
         * são mescladas, pois o código seguinte depende dos dois objetos
         * identificados corretamente em apenas uma máscara para
         * funcionar corretamente.
         */
        var imLogica = Image.logical(imGray);
        var imLogicaPilha = Image.logical(Image.rgb2gray(imRGB));

        for(int i = 0; i < imGray.length; i++){
            for(int j = 0; j < imGray[0].length; j++){
                imLogicaPilha[i][j] = !imLogicaPilha[i][j];
            }
        }

        /*
         * Erodindo e dilatando imagem para remoção de ruído
         * na máscara de identificação da pilha.
         */
        imLogicaPilha = Image.bwOpen(
            imLogicaPilha,
            20
        );

        /*
         * Fazendo identificação da largura da pilha na imagem
         * logo, porque é preciso mesclá-la de imediato a máscara
         * principal para não obter erros na contagem de regiões.
         */
        var fgPixelsPilha = new boolean[imLogica[0].length];
        for(int i = 0; i < imLogica.length; i++){
            for(int j = 0; j < imLogica[0].length; j++){
                if(imLogicaPilha[i][j] && !fgPixelsPilha[j])
                    fgPixelsPilha[j] = true;
            }
        }

        // Guardando índices de início e de fim das colunas correspondentes a pilha
        int inicioPilha = -1, fimPilha = -1;
        for(int i = fgPixelsPilha.length-1; i > 0; i--){
            if(fgPixelsPilha[i]){
                if(inicioPilha == -1)
                    inicioPilha = i;
                if(fgPixelsPilha[i-1] == false){
                    fimPilha = i;
                    break;
                }
            }
        }
        
        totalColunasPilha = inicioPilha-fimPilha+1;

        // Mesclando as duas máscaras
        for(int i = 0; i < imLogica.length; i++){
            for(int j = inicioPilha; j <= fimPilha; j++){
                imLogica[i][j] = imLogicaPilha[i][j];
            }
        }

        // Dilatação e erosão para atenuação dos brilhos existentes na imagem
        imLogica = Image.bwClose(
            imLogica,
            15
        );

        // Erosão e dilatação para remoção do máximo de ruídos possível
        imLogica = Image.bwOpen(
            imLogica,
            40
        );

        // INICIO CONTAGEM DE REGIOES
        // Matriz de rotulos para rotular cada objeto da região de interesse
        int[][] matrizRotulos = new int[imLogica.length][imLogica[0].length];
        
        // Inicialização da matriz
        for(int i = 0; i < imLogica.length; i++){
            for(int j = 0; j < imLogica[0].length; j++){
                matrizRotulos[i][j] = 0;
            }
        }

        // Variável que conta a quantidade de objetos e é utilizada para rotular os mesmos
        int rotulo = 0;

        for(int i = 0; i < imLogica.length; i++){
            for(int j = 0; j < imLogica[0].length; j++){
                /*
                 * Ao encontrar uma região de interesse rotula o objeto
                 * utilizando a função "mapearRegiao" que faz uma busca
                 * em largura na máscara e rotula todo o objeto na
                 * matriz de rótulos
                 */
                if(imLogica[i][j] && matrizRotulos[i][j] == 0){
                    rotulo += 1;
                    matrizRotulos = imagem.mapearRegiao(imLogica, i, j, rotulo, matrizRotulos);
                }
            }
        }
        System.out.println("Quantidade de regioes: "+ rotulo);

        // FIM CONTAGEM DE REGIOES

        /*
         * Algoritmo para remover quaisquer resquícios
         * de objetos de foreground indesejados ou errôneos,
         * mantendo apenas os dois maiores objetos da máscara:
         * a formiga e a pilha.
         */
        if(rotulo > 2){
            int[] qntPixelPorRegiao = new int[rotulo];

            for(int i = 0; i < qntPixelPorRegiao.length; i++)
                qntPixelPorRegiao[i] = 0;

            for(int i = 0; i < imLogica.length; i++){
                for(int j = 0; j < imLogica[0].length; j++){
                    if(matrizRotulos[i][j] != 0){
                        qntPixelPorRegiao[matrizRotulos[i][j] -1] += 1;
                    }
                }
            }

            int primeiroObjeto = 0, rotuloPrimeiroObjeto = 0;
            for(int i = 0; i < qntPixelPorRegiao.length; i++){
                if(qntPixelPorRegiao[i] > primeiroObjeto){
                    primeiroObjeto = qntPixelPorRegiao[i];
                    rotuloPrimeiroObjeto = i + 1;
                }
            }

            qntPixelPorRegiao[rotuloPrimeiroObjeto -1] = 0;

            int segundoObjeto = 0, rotuloSegundoObjeto = 0;
            for(int i = 0; i < qntPixelPorRegiao.length; i++){
                if(qntPixelPorRegiao[i] > segundoObjeto){
                    segundoObjeto = qntPixelPorRegiao[i];
                    rotuloSegundoObjeto = i + 1;
                }
            }

            for(int i = 0; i < imLogica.length; i++){
                for(int j = 0; j < imLogica[0].length; j++){
                    if(matrizRotulos[i][j] != rotuloPrimeiroObjeto && matrizRotulos[i][j] != rotuloSegundoObjeto){
                        imLogica[i][j] = false;
                    }
                }
            }
        }

        // // Gerando imagem final com tons de cinza
        // for(int i = 0; i < imGray.length; i++)
        //     for(int j = 0; j < imGray[0].length; j++)
        //         if(!imLogica[i][j])
        //             imGray[i][j] = 0;
        
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
        for(int i = 0; i < imLogica.length; i++){
            for(int j = 0; j < imLogica[0].length; j++){
                if(imLogica[i][j] && !fgPixels[j])
                    fgPixels[j] = true;
            }
        }
        
        // Guardando índices de início e de fim das colunas correspondentes a cabeça
        int inicioCabeca = -1, fimCabeca = -1;
        for(int i = 0; i < fgPixels.length - 2; i++){
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
                if(j - 1 >= 0){
                    if(imLogica[i][j] && !imLogica[i][j-1]){
                        regioesFrente++;
                        if(regioesFrente == 2){
                            duasRegioes = true;
    
                            indiceVertice[0] = i;
                            indiceVertice[1] = j;
                        }
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

        int[] indicesAltura = {
            indiceVertice[0],
            indiceVertice[1],
            ultimoPixel[0],
            ultimoPixel[1]
        };
        int[] indicesLargura = {
            indiceVertice[0],
            inicioCabeca,
            indiceVertice[0],
            fimCabeca
        };
        int meioDaImagem = imGray.length / 2;
        int[] indicesMetrica = {
            meioDaImagem,
            inicioPilha,
            meioDaImagem,
            fimPilha
        };

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        DecimalFormat df = new DecimalFormat("#.##", symbols);

        String alturaVerticeMandibulaStr = df.format(alturaVerticeMandibulaMm);
        String larguraCabecaStr = df.format(larguraCabecaMm);

        im.setMascara(Image.bw2rgb(imLogica));

        im.setIndicesAltura(indicesAltura);
        im.setIndicesLargura(indicesLargura);
        im.setIndicesMetrica(indicesMetrica);

        im.setDescritores("Largura da cabeça: "+larguraCabecaStr+"mm");
        im.setDescritores(im.getDescritores() + "|Vértice ao fim da mandíbula: "+alturaVerticeMandibulaStr+"mm");
        //im.setDescritores(im.getDescritores() + "|"+stringHist);
        
        
        im.setImagemFinal(imRGB);

        return im;
    }
}

// // Convertendo a imagem pra Mat para utilizar a lib OpenCV
// Mat imCsv = imagem.convertToOpenCV(imRGB);

// double alpha = 1.5; // Fator de contraste
// double beta = 0; // Viés do brilho
// imCsv.convertTo(imCsv, -1, alpha, beta);

// imagem.convertToInt(imCsv);



// var imLogicaErros = Image.logical(imGray);

// for(int i = 0; i < imGray.length; i++)
//     for(int j = 0; j < imGray[0].length; j++)
//         if(imLogicaErros[i][j])
//             imLogica[i][j] = false;

// imLogica = Image.bwClose(
//     imLogica,
//     9
// );