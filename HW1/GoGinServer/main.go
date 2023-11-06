package main

import (
	"bytes"
	"encoding/json"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"io"
	"net/http"
	"strconv"
)

type album struct {
	Artist string `json:"artist"`
	Title  string `json:"title"`
	Year   string `json:"Year"`
}

// Associate a string of AlbumID to it's album
var albumMap = make(map[string]album)

// Associate a stringID to its image size
var imageMetadata = make(map[string]string)

func PostAlbum(c *gin.Context) {
	//Check if there are no errors in the form request
	err := c.Request.ParseMultipartForm(10 << 20)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Unable to parse form"})
		return
	}

	//Generate a new unique album ID
	albumID := uuid.New().String()
	profileJSON := c.PostForm("profile")
	// create an album to store the data
	var newAlbum album
	//unmarshall the data based on fields
	err = json.Unmarshal([]byte(profileJSON), &newAlbum)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Unable to get profile data"})
		return
	}
	//map albumID to albumdata in albumMap
	albumMap[albumID] = newAlbum

	//Read image file
	file, _, err := c.Request.FormFile("image")
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Unable to get image file"})
		return
	}
	defer file.Close()

	//Use the image file to calculate the size of the image
	var imageBuffer bytes.Buffer
	_, err = io.Copy(&imageBuffer, file)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to read image data"})
		return
	}
	imageBytes := imageBuffer.Bytes()
	//convert to int
	imageSizeInt := int64(len(imageBytes))
	//convert to string
	imageSize := strconv.FormatInt(imageSizeInt, 10)

	//Map albumID to imagesize in imageMetadata
	imageMetadata[albumID] = imageSize

	c.JSON(http.StatusOK, gin.H{"ID": albumID, "imageSize": imageSize})
}

func getAlbumByID(c *gin.Context) {
	albumID := c.Param("id")
	albumValue, exists := albumMap[albumID]
	if exists {
		c.JSON(http.StatusOK, albumValue)
	} else {
		c.JSON(http.StatusNotFound, gin.H{"error": "Album not found"})
	}
}

func main() {
	newAlbum := album{
		Artist: "Sex Pistols",
		Title:  "Never Mind The Bollocks",
		Year:   "1977",
	}
	albumID := "1"
	albumMap[albumID] = newAlbum

	router := gin.Default()
	router.Use(gin.Logger())

	router.GET("/albums/:id", getAlbumByID)
	router.POST("/albums", PostAlbum)

	err := router.Run(":8080")
	if err != nil {
		return
	}
}
