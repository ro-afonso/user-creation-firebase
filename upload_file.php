<?php

ini_set('max_execution_time', 120);

$target_dir = "images/";
$target_file = $target_dir . basename($_FILES["file"]["name"]);
$uploadOk = 1;
$imageFileType = pathinfo($target_file, PATHINFO_EXTENSION);
$check = getimagesize($_FILES["file"]["tmp_name"]);
if ($check !== false) {
    $uploadOk = 1;
} else {
    $uploadOk = 0;
}

if (file_exists($target_file)) {
    $uploadOk = 0;
}
if (
    $imageFileType != "jpg" && $imageFileType != "png" && $imageFileType !=
    "jpeg"
    && $imageFileType != "gif"
) {
    $uploadOk = 0;
}
if ($uploadOk == 0) {
    $this->response(['status' => "failed"]);
} else {
    if (move_uploaded_file($_FILES["file"]["tmp_name"], $target_file)) {
        echo json_encode(['status' => "success", "filename" =>
        basename($_FILES["file"]["name"])]);
    } else {
        $this->response(['status' => "failed"]);
    }
}
?>