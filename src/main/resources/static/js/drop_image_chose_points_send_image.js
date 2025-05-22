document.addEventListener('DOMContentLoaded', function () {
    const dropZone = document.getElementById('drop-zone');
    const inputField = document.getElementById('input-file');
    const inputPoints = document.getElementById('input-points');
    const fullscreenContainer = document.getElementById('fullscreen-container');
    const fullscreenImage = document.getElementById('fullscreen-image');
    const clearButton = document.getElementById('clear-button');
    const wrapper = document.getElementById('image-wrapper');
    const reader = new FileReader();

    function loadImageEvent(e) {
        fullscreenImage.src = e.target.result;
        fullscreenContainer.style.display = 'flex';
        fullscreenImage.onload = function () {
            fullscreenImage.dataset.realWidth = fullscreenImage.naturalWidth;
            fullscreenImage.dataset.realHeight = fullscreenImage.naturalHeight;
        };
    }

    function updatePointsInput() {
        let points = [];
        document.querySelectorAll('.point-marker')
            .forEach(div => points.push(
                {x: parseInt(div.dataset.normX), y: parseInt(div.dataset.normY)}));
        inputPoints.value = JSON.stringify(points);
    }

    function handleDragOver(e) {
        e.preventDefault();
    }

    function openImageOnDrop(e) {
        e.preventDefault();
        if (e.dataTransfer.files.length > 0) {
            inputField.files = e.dataTransfer.files;
            reader.readAsDataURL(e.dataTransfer.files[0]);
        }
    }

    function openImageOnChoose() {
        reader.readAsDataURL(inputField.files[0]);
    }

    function deletePointFromImage(e) {
        e.preventDefault();
        wrapper.removeChild(this);
        updatePointsInput();
    }

    function addPointOnImage(e) {
        const rect = fullscreenImage.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        const normX = Math.round((x / rect.width) * fullscreenImage.dataset.realWidth);
        const normY = Math.round((y / rect.height) * fullscreenImage.dataset.realHeight);
        const div = document.createElement('div');
        const radius = 3;
        div.className = 'point-marker';
        div.style.position = 'absolute';
        div.style.backgroundImage = 'linear-gradient(red, lightgoldenrodyellow)';
        div.style.borderRadius = '50%';
        div.style.width = 2 * radius + '%';
        div.style.height = 2 * radius + '%';
        div.style.left = (x / rect.width * 100) - radius + '%';
        div.style.top = (y / rect.height * 100) - radius + '%';
        div.dataset.normX = String(normX);
        div.dataset.normY = String(normY);
        div.addEventListener('contextmenu', deletePointFromImage);
        wrapper.appendChild(div);
        updatePointsInput();
    }

    function cleanAllPoints() {
        document.querySelectorAll('.point-marker').forEach(el => el.remove());
        updatePointsInput();
    }

    reader.addEventListener('load', loadImageEvent);
    dropZone.addEventListener('dragenter', handleDragOver);
    dropZone.addEventListener('dragover', handleDragOver);
    dropZone.addEventListener('drop', openImageOnDrop);
    inputField.addEventListener('change', openImageOnChoose);
    fullscreenImage.addEventListener('click', addPointOnImage);
    clearButton.addEventListener('click', cleanAllPoints);
});