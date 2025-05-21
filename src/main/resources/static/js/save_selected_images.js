function saveSelectedImages() {
    const selectedCheckboxes = document.querySelectorAll('.checkbox:checked');

    if (!selectedCheckboxes.length) return alert('No images are selected.');

    let zip = new JSZip(); // создаем экземпляр JSZip

    for (let i = 0; i < selectedCheckboxes.length; i++) {
        const checkbox = selectedCheckboxes[i];
        const base64Data = checkbox.dataset.base64;
        const filename = `image-${i + 1}.png`;

        // Преобразование Base64 в массив байтов
        const binaryString = window.atob(base64Data);
        const arrayBuffer = new ArrayBuffer(binaryString.length);
        const uintArray = new Uint8Array(arrayBuffer);
        for (let j = 0; j < binaryString.length; j++) {
            uintArray[j] = binaryString.charCodeAt(j);
        }

        // Добавление файла в архив
        zip.file(filename, uintArray);
    }

    // Генерация ZIP-файла и предложение пользователю скачать его
    zip.generateAsync({type: 'blob'}).then(function (blob) {
        window.saveAs(blob, "images.zip");
    });
}