# Wi-Fi Brute Lab

APK didático para demonstrar busca exaustiva sem realizar tentativas de conexão.

## Garantias de segurança

- Não declara permissões de Wi-Fi, Internet ou localização.
- Não enumera SSIDs e não chama APIs de conexão.
- O “nome da rede” é somente um rótulo exibido na tela.
- A senha-alvo fica na memória durante a execução e não é persistida.
- O espaço de busca é limitado a 3 milhões de candidatos para evitar uso excessivo do aparelho.

## Compilar

```sh
cd /data/data/com.termux/files/home/projects/wifi-brute-lab
./build.sh
```

O APK assinado será gerado em `build/WiFi-Brute-Lab.apk`.

## Ideia para o trabalho

O app percorre candidatos em ordem lexicográfica, começando por strings de tamanho 1. Para um
alfabeto de tamanho `b` e comprimento máximo `m`, o pior caso é `b + b² + ... + bᵐ`, portanto
`O(bᵐ)`. As métricas permitem comparar o crescimento do espaço, o tempo e a taxa média de
tentativas.
