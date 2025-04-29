document.addEventListener("DOMContentLoaded", function () {
    const drop_zone = document.getElementById("drop-zone");
    const send_button = document.getElementById("send-button");
    const input_field = document.getElementById("input-file");
    const img_container = document.getElementById("img-container");

    function handle_drag_over(e) {
        e.preventDefault();
    }

    function update_button_state() {
        send_button.disabled = !(input_field.files && input_field.files.length > 0);
    }

    function update_image_state() {
        while (img_container.firstChild) {
            img_container.removeChild(img_container.lastChild);
        }
        if (input_field.files.length > 0) {
            Array.from(input_field.files).forEach((file, index) => {
                const objectUrl = URL.createObjectURL(file); // Temporary reference to the image
                const sub_div = document.createElement('div');
                sub_div.className = 'sub-div';
                sub_div.style.backgroundImage = `url("${objectUrl}")`;
                img_container.appendChild(sub_div);
            });
        }
    }

    drop_zone.addEventListener("dragenter", handle_drag_over);

    drop_zone.addEventListener("dragover", handle_drag_over);

    drop_zone.addEventListener("drop", e => {
        e.preventDefault();
        if (e.dataTransfer.files.length > 0) {
            input_field.files = e.dataTransfer.files;
            update_button_state();
            update_image_state();
        }
    });

    input_field.addEventListener("change", () => {
        update_button_state();
        update_image_state();
    });

});