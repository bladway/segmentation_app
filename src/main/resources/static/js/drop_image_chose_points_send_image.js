document.addEventListener('DOMContentLoaded', function () {
    const dropZone = document.getElementById('drop-zone');
    const inputField = document.getElementById('input-file');
    const fullscreenContainer = document.getElementById('fullscreen-container');
    const fullscreenImage = document.getElementById('fullscreen-image');
    const clearButton = document.getElementById('clear-button');
    const sendButton = document.getElementById('send-button');
    let pointsArray = []; // Массив для хранения координат выбранных точек
    function handleDragOver(e) {
        e.preventDefault();
    }

    function loadFullScreenImage(file) {
        const reader = new FileReader();
        reader.onload = function (event) {
            fullscreenImage.src = event.target.result;
            fullscreenImage.style.display = 'block';
            fullscreenContainer.style.display = 'flex';
        };
        reader.readAsDataURL(file);
    }

    function addPoint(x, y) {
        const div = document.createElement('div');
        div.className = 'point-marker';
        div.style.position = 'absolute';
        div.style.backgroundColor = 'red';
        div.style.borderRadius = '50%';
        div.style.width = '50px';
        div.style.height = '50px';
        div.style.transform = `translate(${x - 25}px, ${y - 25}px)`; // Центруем маркер над точкой
        fullscreenContainer.appendChild(div);
        pointsArray.push({x, y});
    }

    function clearAllPoints() {
        while (fullscreenContainer.lastElementChild && fullscreenContainer.lastElementChild.classList.contains('point-marker')) {
            fullscreenContainer.removeChild(fullscreenContainer.lastElementChild);
        }
        pointsArray = [];
    }

    function submitForm() {
        const formData = new FormData(document.getElementById('form'));
        formData.append('points', JSON.stringify(pointsArray)); // Добавляем список точек в форму
        fetch('/manual_segmentation', {
            method: 'POST',
            body: formData,
        })
            .then((response) => response.json())
            .then((data) => console.log(data)) // Логируем полученный ответ сервера
            .catch((err) => console.error(err));
    }

    dropZone.addEventListener('dragenter', handleDragOver);
    dropZone.addEventListener('dragover', handleDragOver);
    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        if (e.dataTransfer.files.length > 0) {
            inputField.file = e.dataTransfer.files[0];
            loadFullScreenImage(e.dataTransfer.files[0]); // Показываем полноэкранное изображение
        }
    });
    fullscreenImage.addEventListener('click', (e) => {
        const rect = fullscreenImage.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        addPoint(x, y);
    });
    clearButton.addEventListener('click', clearAllPoints);
    sendButton.addEventListener('click', submitForm);
});