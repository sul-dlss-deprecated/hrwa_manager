#!/usr/bin/env ruby

require "rubygems"
require "bundler/setup"

# require any other gems here
require "image_science"

# require necessary files
require_relative 'image_derivative_generator.rb'

if(ARGV.length == 0 || ARGV[0] == '--help')
  #print_usage
  ImageDerivativeGenerator::printUsage()
  exit(0)
else
  file_or_directory_to_process = ARGV[0]
  output_directory = ARGV[1]
  new_width = ARGV[2]
  image_type_extension = ARGV[3]

  imageDerivativeGenerator = ImageDerivativeGenerator.new(file_or_directory_to_process, output_directory, new_width, image_type_extension)
end
