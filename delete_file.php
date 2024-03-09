<?php
if(isset($_POST['deleteImage'])) {

   $imageName = $_POST['deleteImage'];
   $filepath = __DIR__ . "/images/" . $imageName;
   
   if (file_exists($filepath)){
      unlink($filepath);
   }
   else{
      http_response_code(404);
   }
}
?>