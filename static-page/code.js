function readImage(input) {
    if (input.files && input.files[0]) {
        const reader = new FileReader();

        reader.onload = function (e) {
            const img = new Image();
            
            img.onload = async function () {
                const canvas = document.getElementById('canvas');
                const ctx = canvas.getContext('2d', {willReadFrequently: true});
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

                console.log(resultado)
                
                let descritores = resultado['descritores'].split('|')
                
                desenhar(resultado['mascara'], 'mascara')
                desenhar(resultado['imagemFinal'], 'imagemFinal')

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

    if(isNaN(metricInput)){
        metricInput = 6.8
    }

    imageDict = {
        "imagem": img,
        "metrica": metricInput
    }

    const url = 'http://localhost:8080/imagem/'

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
    let requisicao = await fetch('http://localhost:8080/imagem/');
    return await requisicao.json()
}

function desenhar(im, idCanvas){
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

// Associando a função ao evento change do input file
document.getElementById('fileInput').addEventListener('change', function () {
    readImage(this);
});