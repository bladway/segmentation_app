document.addEventListener('DOMContentLoaded', function () {
    const dropZone = document.getElementById('drop-zone');
    const sendButton = document.getElementById('send-button');
    const inputField = document.getElementById('input-file');
    const imgContainer = document.getElementById('img-container');

    function handleDragOver(e) {
        e.preventDefault();
    }

    function updateButtonState() {
        sendButton.disabled = !(inputField.files && inputField.files.length > 0);
    }

    function updateImageState() {
        while (imgContainer.firstChild) {
            imgContainer.removeChild(imgContainer.lastChild);
        }
        if (inputField.files.length > 0) {
            Array.from(inputField.files).forEach((file) => {
                const objectUrl = URL.createObjectURL(file);
                const subDiv = document.createElement('div');
                subDiv.className = 'sub-div';
                subDiv.style.backgroundImage = `url('${objectUrl}')`;
                imgContainer.appendChild(subDiv);
            });
        }
    }

    function updateButtonAndImageState() {
        updateButtonState();
        updateImageState();
    }

    function loadImagesOnDrop(e) {
        e.preventDefault();
        if (e.dataTransfer.files.length > 0) {
            inputField.files = e.dataTransfer.files;
            updateButtonAndImageState()
        }
    }

    dropZone.addEventListener('dragenter', handleDragOver);
    dropZone.addEventListener('dragover', handleDragOver);
    dropZone.addEventListener('drop', loadImagesOnDrop);
    inputField.addEventListener('change', updateButtonAndImageState);
});