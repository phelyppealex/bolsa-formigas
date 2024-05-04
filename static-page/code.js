function readImage(input) {
    if (input.files && input.files[0]) {
        const reader = new FileReader();

        reader.onload = function (e) {
            const img = new Image();
            
            img.onload = async function () {
                const canvas = document.getElementById('canvas');
                const ctx = canvas.getContext('2d');
                canvas.width = img.width;
                canvas.height = img.height;
                ctx.drawImage(img, 0, 0);
                const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
                const pixelData = imageData.data;
                const width = canvas.width;
                const height = canvas.height;

                // Matriz para armazenar os canais R, G e B
                const rgbMatrix = [];

                // Preenchendo as matrizes com os valores dos pixels
                for (let i = 0; i < height; i++) {
                    rgbMatrix[i] = [];
                    for (let j = 0; j < width; j++) {
                        const index = (i * width + j) * 4;
                        rgbMatrix[i][j] = [pixelData[index], pixelData[index + 1], pixelData[index + 2]]
                    }
                }
                await processarImagem(rgbMatrix)

                let requisicao = await fetch('http://localhost:8080/imagem/');
                let todosDescritores = await requisicao.text()
                let descritores = todosDescritores.split('|')

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

// Associando a função ao evento change do input file
document.getElementById('fileInput').addEventListener('change', function () {
    readImage(this);
});