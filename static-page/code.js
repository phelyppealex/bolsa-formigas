let url = ''
let ctx
function readImage(input) {
    if (input.files && input.files[0]) {
        const reader = new FileReader();

        reader.onload = function (e) {
            const img = new Image();
            
            img.onload = async function () {
                const canvas = document.getElementById('canvas');
                ctx = canvas.getContext('2d', {willReadFrequently: true});
                canvas.width = img.width;
                canvas.height = img.height;
                ctx.drawImage(img, 0, 0);
                const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
                const pixelData = imageData.data;
                const width = canvas.width;
                const height = canvas.height;

                // Matriz para armazenar os canais R, G e B
                const rgbMatrix = [];
                //canvas.getContext('2d').getImageData(0, 0, canvas.width, canvas.height).data;

                // Preenchendo as matrizes com os valores dos pixels
                for (let i = 0; i < height; i++) {
                    rgbMatrix[i] = [];
                    for (let j = 0; j < width; j++) {
                        const index = (i * width + j) * 4;
                        rgbMatrix[i][j] = [pixelData[index], pixelData[index + 1], pixelData[index + 2]]
                    }
                }
                
                await processarImagem(rgbMatrix)
                let resultado = await obterDados()

                let descritores = resultado['descritores'].split('|')
                console.log(resultado)
                if(resultado['mascara'] != null){
                    desenharImagem(resultado['mascara'], 'mascara')
                }
                if(resultado['imagemFinal'] != null){
                    desenharImagem(resultado['imagemFinal'], 'imagemFinal')
                }
                if(resultado['imagemContrastada'] != null){
                    desenharImagem(resultado['imagemContrastada'], 'imagemContrastada')
                }
                if(resultado['indicesLargura'] != null){
                    desenharTraco(resultado['indicesLargura'], 'cyan')
                    
                }
                if(resultado['indicesAltura'] != null){
                    desenharTraco(resultado['indicesAltura'], 'yellow')
                }
                if(resultado['indicesMetrica'] != null){
                    desenharTraco(resultado['indicesMetrica'], 'red')
                }

                for(let d of descritores){
                    let lista = document.getElementById('listaDescritores')
                    let linha = document.createElement('li')
                    let texto = document.createElement('p')

                    lista.appendChild(linha)
                    linha.appendChild(texto)
                    texto.textContent = d
                }
                
                console.log(descritores)
            }
            img.src = e.target.result;
        }
        reader.readAsDataURL(input.files[0]);
    }
}

async function processarImagem(img){
    let metricInput = parseFloat(document.getElementById('metricInput').value)
    let imageTypeInput = document.getElementsByName('tipo_medicao')
    let imageType = ''
    
    for(let radio of imageTypeInput){
        if(radio.checked){
            imageType = radio.value
        }
    }

    // Caso uma nova métrica não seja definida pelo cliente, a padrão é posta automaticamente
    if(isNaN(metricInput)){
        metricInput = 6.8
    }

    imageDict = {
        "imagem": img,
        "metrica": metricInput,
        "tipoImagem": imageType
    }

    if(imageType === 'cabeca'){
        url = 'http://localhost:8080/imagem-capsula/'
    }else{
        url = 'http://localhost:8080/imagem-mandibula/'
    }
    

    let requisicao = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(imageDict)
    }).then( (response) => {
        if(!response.ok){
            throw new Error('Erro ao processar imagem')
        }
    })
}

async function obterDados(){
    let requisicao = await fetch(url);
    return await requisicao.json()
}

function desenharImagem(im, idCanvas){
    const canvas = document.getElementById(idCanvas);
    let ctx = canvas.getContext('2d', {willReadFrequently: true});

    canvas.height = im.length
    canvas.width = im[0].length
    let width = canvas.width
    let height = canvas.height

    for(let i = 0; i < height; i++){
        for(let j = 0; j < width; j++){
            ctx.fillStyle = 'rgba('+im[i][j][0]+', '+im[i][j][1]+', '+im[i][j][2]+', 255)'
            ctx.fillRect(j, i, 1, 1);   
        }
    }
}

function desenharTraco(indices, cor){
    // Desenhando traço na imagem
    ctx.strokeStyle = cor; // Cor do traço
    ctx.lineWidth = 13; // Largura do traço
    ctx.beginPath();
    ctx.moveTo(indices[1], indices[0]); // Move para o ponto inicial
    ctx.lineTo(indices[3], indices[2]); // Cria uma linha até o ponto final
    ctx.stroke(); // Desenha o traço na tela
}

// Associando a função ao evento change do input file
document.getElementById('fileInput').addEventListener('change', function () {
    readImage(this);
});